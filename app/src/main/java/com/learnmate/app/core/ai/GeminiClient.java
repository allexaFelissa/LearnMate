package com.learnmate.app.core.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.learnmate.app.BuildConfig;
import com.learnmate.app.core.utils.AppExecutors;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Shared Gemini REST API client used by Person 1 (Summaries & Flashcards)
 * and Person 2 (Quiz generation).
 *
 * Features:
 * - IPv4-first DNS (prevents Android Emulator IPv6 stalls)
 * - Disables thinking tokens (thinkingBudget = 0) on 2.5 Flash models for fast JSON responses
 * - Automatic model discovery + fallback chain on 503 (overloaded), 429, 404, or timeouts
 */
public class GeminiClient {

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_MODELS_TO_TRY = 4;

    private static final List<String> DEFAULT_FALLBACK_MODELS = Arrays.asList(
            "models/gemini-2.5-flash-lite",
            "models/gemini-2.5-flash",
            "models/gemini-2.0-flash-lite",
            "models/gemini-2.0-flash"
    );

    private static volatile GeminiClient instance;
    private final OkHttpClient httpClient;

    private volatile List<String> cachedCandidateModels = null;
    private volatile String lastUsedModel = "";

    public static GeminiClient getInstance() {
        if (instance == null) {
            synchronized (GeminiClient.class) {
                if (instance == null) {
                    instance = new GeminiClient();
                }
            }
        }
        return instance;
    }

    private GeminiClient() {
        // Prefer IPv4 addresses first to avoid IPv6 timeout stalls on Android Emulators
        Dns ipv4FirstDns = hostname -> {
            List<InetAddress> all = Dns.SYSTEM.lookup(hostname);
            List<InetAddress> ordered = new ArrayList<>(all.size());
            for (InetAddress addr : all) {
                if (addr instanceof Inet4Address) ordered.add(addr);
            }
            for (InetAddress addr : all) {
                if (!(addr instanceof Inet4Address)) ordered.add(addr);
            }
            return ordered;
        };

        this.httpClient = new OkHttpClient.Builder()
                .dns(ipv4FirstDns)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .build();
    }

    public String getLastUsedModel() {
        return lastUsedModel;
    }

    /**
     * Generates a response from Gemini asynchronously with automatic failover across models.
     *
     * @param prompt     Full prompt text.
     * @param expectJson If true, sets responseMimeType to application/json.
     * @param callback   Callback invoked on the Main UI thread.
     */
    public void generateContent(String prompt, boolean expectJson, AiCallback<String> callback) {
        String apiKey = sanitizeApiKey(BuildConfig.GEMINI_API_KEY);
        if (apiKey.isEmpty() || "YOUR_GEMINI_API_KEY_HERE".equals(apiKey)) {
            postError(callback, "Gemini API key is missing. Add GEMINI_API_KEY to local.properties.");
            return;
        }

        AppExecutors.getInstance().networkIO().execute(() -> {
            List<String> candidates = getOrDiscoverModels(apiKey);
            int limit = Math.min(candidates.size(), MAX_MODELS_TO_TRY);
            List<String> failureLog = new ArrayList<>();

            for (int i = 0; i < limit; i++) {
                String model = candidates.get(i);
                String shortName = model.replace("models/", "");
                boolean disableThinking = shouldDisableThinking(model);

                try {
                    ApiCallResult result = executeGenerate(model, apiKey, buildRequestPayload(prompt, expectJson, disableThinking));

                    // If this specific model rejects thinkingConfig with 400, retry once without thinkingConfig
                    if (!result.successful && result.code == 400 && disableThinking
                            && result.body.toLowerCase(Locale.US).contains("thinking")) {
                        result = executeGenerate(model, apiKey, buildRequestPayload(prompt, expectJson, false));
                    }

                    if (result.successful) {
                        String text = extractCandidateText(result.body);
                        if (text != null && !text.trim().isEmpty()) {
                            lastUsedModel = shortName;
                            promoteModel(model);
                            String cleaned = expectJson ? stripCodeFences(text) : text.trim();
                            AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(cleaned));
                            return;
                        }
                        failureLog.add(shortName + ": empty response");
                    } else {
                        String errMsg = extractErrorMessage(result.body);
                        // Auth/key errors won't be fixed by trying other models
                        if (result.code == 401 || result.code == 403) {
                            postError(callback, "Gemini API auth error (" + result.code + "): " + errMsg);
                            return;
                        }
                        failureLog.add(shortName + ": HTTP " + result.code);

                        // Brief backoff before failing over on transient 503/429
                        if ((result.code == 503 || result.code == 429) && i < limit - 1) {
                            sleepQuietly(1200);
                        }
                    }
                } catch (IOException e) {
                    failureLog.add(shortName + ": " + (e.getMessage() != null ? e.getMessage() : "timeout"));
                    if (i < limit - 1) {
                        sleepQuietly(800);
                    }
                } catch (Exception e) {
                    failureLog.add(shortName + ": " + e.getMessage());
                }
            }

            postError(callback, "AI request failed across fallback models (" + String.join(", ", failureLog) + "). Please try again.");
        });
    }

    private ApiCallResult executeGenerate(String modelPath, String apiKey, JsonObject payload) throws IOException {
        String normalizedModel = modelPath.startsWith("models/") ? modelPath : "models/" + modelPath;
        RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(API_BASE + normalizedModel + ":generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseStr = response.body() != null ? response.body().string() : "";
            return new ApiCallResult(response.isSuccessful(), response.code(), responseStr);
        }
    }

    private List<String> getOrDiscoverModels(String apiKey) {
        if (cachedCandidateModels != null && !cachedCandidateModels.isEmpty()) {
            return new ArrayList<>(cachedCandidateModels);
        }

        List<String> discovered = discoverTextModels(apiKey);
        if (!discovered.isEmpty()) {
            cachedCandidateModels = discovered;
            return new ArrayList<>(discovered);
        }
        return new ArrayList<>(DEFAULT_FALLBACK_MODELS);
    }

    private void promoteModel(String workingModel) {
        if (cachedCandidateModels == null || cachedCandidateModels.isEmpty()) return;
        List<String> reordered = new ArrayList<>(cachedCandidateModels.size());
        reordered.add(workingModel);
        for (String m : cachedCandidateModels) {
            if (!m.equals(workingModel)) {
                reordered.add(m);
            }
        }
        cachedCandidateModels = reordered;
    }

    /**
     * Queries /v1beta/models and returns an ordered list prioritizing fast Flash-Lite & Flash models.
     */
    private List<String> discoverTextModels(String apiKey) {
        Request request = new Request.Builder()
                .url(API_BASE + "models")
                .addHeader("x-goog-api-key", apiKey)
                .get()
                .build();

        List<String> flashLite = new ArrayList<>();
        List<String> flashStable = new ArrayList<>();
        List<String> flashOther = new ArrayList<>();
        List<String> otherGemini = new ArrayList<>();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return new ArrayList<>();
            String body = response.body().string();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("models")) return new ArrayList<>();

            for (JsonElement el : root.getAsJsonArray("models")) {
                JsonObject m = el.getAsJsonObject();
                if (!m.has("name") || !supportsGenerateContent(m)) continue;

                String name = m.get("name").getAsString();
                String lower = name.toLowerCase(Locale.US);

                if (!lower.contains("gemini")
                        || lower.contains("embedding")
                        || lower.contains("tts")
                        || lower.contains("image")
                        || lower.contains("audio")
                        || lower.contains("vision")
                        || lower.contains("robotics")
                        || lower.contains("computer-use")
                        || lower.contains("deep-research")) {
                    continue;
                }

                if (lower.contains("flash-lite") && !lower.contains("preview") && !lower.contains("exp")) {
                    flashLite.add(name);
                } else if (lower.contains("flash") && !lower.contains("lite") && !lower.contains("preview") && !lower.contains("exp")) {
                    flashStable.add(name);
                } else if (lower.contains("flash")) {
                    flashOther.add(name);
                } else {
                    otherGemini.add(name);
                }
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }

        Set<String> ordered = new LinkedHashSet<>();
        ordered.addAll(flashLite);
        ordered.addAll(flashStable);
        ordered.addAll(flashOther);
        ordered.addAll(otherGemini);
        return new ArrayList<>(ordered);
    }

    private boolean shouldDisableThinking(String modelName) {
        String lower = modelName.toLowerCase(Locale.US);
        return lower.contains("2.5") && lower.contains("flash");
    }

    private boolean supportsGenerateContent(JsonObject modelObj) {
        if (!modelObj.has("supportedGenerationMethods")) return false;
        JsonArray methods = modelObj.getAsJsonArray("supportedGenerationMethods");
        for (JsonElement el : methods) {
            if ("generateContent".equals(el.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private String sanitizeApiKey(String raw) {
        if (raw == null) return "";
        String key = raw.trim();
        if ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'"))) {
            key = key.substring(1, key.length() - 1).trim();
        }
        return key;
    }

    private JsonObject buildRequestPayload(String prompt, boolean expectJson, boolean disableThinking) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject payload = new JsonObject();
        payload.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", expectJson ? 0.3 : 0.6);
        generationConfig.addProperty("maxOutputTokens", 2048);
        if (expectJson) {
            generationConfig.addProperty("responseMimeType", "application/json");
        }
        if (disableThinking) {
            JsonObject thinkingConfig = new JsonObject();
            thinkingConfig.addProperty("thinkingBudget", 0);
            generationConfig.add("thinkingConfig", thinkingConfig);
        }
        payload.add("generationConfig", generationConfig);

        return payload;
    }

    private String extractCandidateText(String responseJson) {
        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
        if (!root.has("candidates")) return null;
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) return null;

        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
        if (!firstCandidate.has("content")) return null;
        JsonObject content = firstCandidate.getAsJsonObject("content");
        if (!content.has("parts")) return null;
        JsonArray parts = content.getAsJsonArray("parts");
        if (parts == null || parts.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            JsonObject part = parts.get(i).getAsJsonObject();
            if (part.has("text")) {
                sb.append(part.get("text").getAsString());
            }
        }
        return sb.toString();
    }

    private String extractErrorMessage(String responseJson) {
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
            if (root.has("error") && root.getAsJsonObject("error").has("message")) {
                return root.getAsJsonObject("error").get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return "Request failed";
    }

    private String stripCodeFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void postError(AiCallback<String> callback, String message) {
        AppExecutors.getInstance().mainThread().execute(() -> callback.onError(message));
    }

    private static class ApiCallResult {
        final boolean successful;
        final int code;
        final String body;

        ApiCallResult(boolean successful, int code, String body) {
            this.successful = successful;
            this.code = code;
            this.body = body;
        }
    }
}

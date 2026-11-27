package com.learnmate.app.core.ai;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.learnmate.app.core.db.dao.SummaryDao;
import com.learnmate.app.core.utils.AppExecutors;
import com.learnmate.app.model.Material;
import com.learnmate.app.model.Summary;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates and caches structured AI study summaries for a Material using GeminiClient.
 */
public class SummaryAiService {

    private static final int MAX_PROMPT_CHARS = 14_000;

    private final SummaryDao summaryDao;
    private final Gson gson = new Gson();

    public SummaryAiService(Context context) {
        this.summaryDao = new SummaryDao(context.getApplicationContext());
    }

    /**
     * Loads cached summary if available and forceRegenerate is false; otherwise calls Gemini API.
     */
    public void getOrGenerateSummary(Material material, String style, boolean forceRegenerate, AiCallback<Summary> callback) {
        if (material == null) {
            callback.onError("Material not found.");
            return;
        }
        String resolvedStyle = (style == null || style.trim().isEmpty()) ? Summary.STYLE_CONCISE : style;

        AppExecutors.getInstance().diskIO().execute(() -> {
            if (!forceRegenerate) {
                Summary cached = summaryDao.getSummary(material.getId(), resolvedStyle);
                if (cached != null) {
                    AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(cached));
                    return;
                }
            }

            String text = material.getExtractedText();
            if (text == null || text.trim().isEmpty()) {
                AppExecutors.getInstance().mainThread().execute(() ->
                        callback.onError("This material has no extracted text to summarize."));
                return;
            }

            String prompt = buildPrompt(material.getTitle(), resolvedStyle, truncateForPrompt(text));
            GeminiClient.getInstance().generateContent(prompt, true, new AiCallback<String>() {
                @Override
                public void onSuccess(String jsonResponse) {
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        try {
                            SummaryPayload payload = gson.fromJson(jsonResponse, SummaryPayload.class);
                            if (payload == null || payload.overview == null) {
                                AppExecutors.getInstance().mainThread().execute(() ->
                                        callback.onError("Could not parse summary from AI response."));
                                return;
                            }

                            Summary summary = new Summary();
                            summary.setMaterialId(material.getId());
                            summary.setStyle(resolvedStyle);
                            summary.setOverview(payload.overview.trim());
                            summary.setKeyPoints(payload.keyPoints != null ? payload.keyPoints : new ArrayList<>());
                            summary.setKeyTerms(payload.keyTerms != null ? payload.keyTerms : new ArrayList<>());
                            summary.setGeneratedAt(System.currentTimeMillis());

                            summaryDao.upsertSummary(summary);
                            AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(summary));
                        } catch (Exception e) {
                            AppExecutors.getInstance().mainThread().execute(() ->
                                    callback.onError("Failed to parse AI summary JSON: " + e.getMessage()));
                        }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    callback.onError(errorMessage);
                }
            });
        });
    }

    private String buildPrompt(String title, String style, String materialText) {
        String styleInstruction;
        if (Summary.STYLE_DETAILED.equals(style)) {
            styleInstruction = "Create a thorough, detailed study summary with 7-10 key points and 6-10 key terms.";
        } else if (Summary.STYLE_EXAM_CHEAT_SHEET.equals(style)) {
            styleInstruction = "Create a high-yield exam cheat sheet focusing on formulas, core rules, common pitfalls, and definitions (6-8 key points and 6-8 key terms).";
        } else {
            styleInstruction = "Create a concise, easy-to-scan summary with 4-6 key points and 4-6 key terms.";
        }

        return "You are LearnMate, an expert AI study assistant. " + styleInstruction + "\n"
                + "Write everything in clear English based strictly on the provided study material.\n"
                + "Return ONLY a valid JSON object matching this exact schema:\n"
                + "{\n"
                + "  \"overview\": \"2-4 sentence executive overview of the material\",\n"
                + "  \"key_points\": [\"Clear takeaway 1\", \"Clear takeaway 2\"],\n"
                + "  \"key_terms\": [{\"term\": \"Term name\", \"definition\": \"Clear student-friendly definition\"}]\n"
                + "}\n\n"
                + "Material Title: " + title + "\n"
                + "Material Content:\n" + materialText;
    }

    private String truncateForPrompt(String text) {
        if (text.length() <= MAX_PROMPT_CHARS) return text;
        return text.substring(0, MAX_PROMPT_CHARS);
    }

    private static class SummaryPayload {
        @SerializedName("overview")
        String overview;

        @SerializedName("key_points")
        List<String> keyPoints;

        @SerializedName("key_terms")
        List<Summary.KeyTerm> keyTerms;
    }
}

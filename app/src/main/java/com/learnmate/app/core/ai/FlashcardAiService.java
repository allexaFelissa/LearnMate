package com.learnmate.app.core.ai;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.learnmate.app.core.db.dao.FlashcardDao;
import com.learnmate.app.core.utils.AppExecutors;
import com.learnmate.app.model.Flashcard;
import com.learnmate.app.model.Material;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates and persists revision flashcards from a Material using GeminiClient.
 */
public class FlashcardAiService {

    private static final int MAX_PROMPT_CHARS = 14_000;
    private static final Type FLASHCARD_PAYLOAD_LIST = new TypeToken<List<FlashcardPayload>>() {}.getType();

    private final FlashcardDao flashcardDao;
    private final Gson gson = new Gson();

    public FlashcardAiService(Context context) {
        this.flashcardDao = new FlashcardDao(context.getApplicationContext());
    }

    /**
     * Calls Gemini to generate `count` flashcards, saves them to SQLite, and returns the saved list.
     *
     * @param replaceExisting If true, deletes existing flashcards for this material before inserting.
     */
    public void generateFlashcards(Material material, int count, boolean replaceExisting, AiCallback<List<Flashcard>> callback) {
        if (material == null) {
            callback.onError("Material not found.");
            return;
        }
        String text = material.getExtractedText();
        if (text == null || text.trim().isEmpty()) {
            callback.onError("This material has no extracted text for flashcard generation.");
            return;
        }

        int safeCount = Math.max(3, Math.min(count, 20));
        String prompt = buildPrompt(material.getTitle(), safeCount, truncateForPrompt(text));

        GeminiClient.getInstance().generateContent(prompt, true, new AiCallback<String>() {
            @Override
            public void onSuccess(String jsonResponse) {
                AppExecutors.getInstance().diskIO().execute(() -> {
                    try {
                        List<FlashcardPayload> payloads = gson.fromJson(jsonResponse, FLASHCARD_PAYLOAD_LIST);
                        if (payloads == null || payloads.isEmpty()) {
                            AppExecutors.getInstance().mainThread().execute(() ->
                                    callback.onError("AI did not return any flashcards. Please try again."));
                            return;
                        }

                        List<Flashcard> cards = new ArrayList<>();
                        for (FlashcardPayload item : payloads) {
                            if (item == null || item.front == null || item.back == null) continue;
                            String topic = (item.topic != null && !item.topic.trim().isEmpty())
                                    ? item.topic.trim() : "General";
                            cards.add(new Flashcard(
                                    material.getId(),
                                    topic,
                                    item.front.trim(),
                                    item.back.trim()
                            ));
                        }

                        if (cards.isEmpty()) {
                            AppExecutors.getInstance().mainThread().execute(() ->
                                    callback.onError("Parsed flashcards were empty. Please regenerate."));
                            return;
                        }

                        if (replaceExisting) {
                            flashcardDao.deleteAllForMaterial(material.getId());
                        }
                        flashcardDao.bulkInsert(cards);

                        List<Flashcard> allSaved = flashcardDao.getFlashcardsByMaterial(material.getId(), false);
                        AppExecutors.getInstance().mainThread().execute(() -> callback.onSuccess(allSaved));
                    } catch (Exception e) {
                        AppExecutors.getInstance().mainThread().execute(() ->
                                callback.onError("Failed to parse flashcards JSON: " + e.getMessage()));
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private String buildPrompt(String title, int count, String materialText) {
        return "You are LearnMate, an expert AI study assistant.\n"
                + "Generate exactly " + count + " high-quality revision flashcards in English from the study material below.\n"
                + "Each flashcard should test a single clear concept, definition, or question.\n"
                + "Return ONLY a valid JSON array matching this exact schema:\n"
                + "[\n"
                + "  {\"topic\": \"Short subtopic name\", \"front\": \"Question or concept prompt\", \"back\": \"Concise accurate answer\"}\n"
                + "]\n\n"
                + "Material Title: " + title + "\n"
                + "Material Content:\n" + materialText;
    }

    private String truncateForPrompt(String text) {
        if (text.length() <= MAX_PROMPT_CHARS) return text;
        return text.substring(0, MAX_PROMPT_CHARS);
    }

    private static class FlashcardPayload {
        @SerializedName("topic")
        String topic;

        @SerializedName("front")
        String front;

        @SerializedName("back")
        String back;
    }
}

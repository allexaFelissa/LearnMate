package com.learnmate.app.ui.flashcard;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.learnmate.app.core.ai.AiCallback;
import com.learnmate.app.core.ai.FlashcardAiService;
import com.learnmate.app.core.db.dao.FlashcardDao;
import com.learnmate.app.core.db.dao.MaterialDao;
import com.learnmate.app.core.navigation.NavExtras;
import com.learnmate.app.core.utils.AppExecutors;
import com.learnmate.app.core.utils.UiUtils;
import com.learnmate.app.databinding.ActivityFlashcardsBinding;
import com.learnmate.app.model.Flashcard;
import com.learnmate.app.model.Material;

import java.util.ArrayList;
import java.util.List;

public class FlashcardsActivity extends AppCompatActivity {

    private ActivityFlashcardsBinding binding;
    private MaterialDao materialDao;
    private FlashcardDao flashcardDao;
    private FlashcardAiService flashcardAiService;

    private Material currentMaterial;
    private final List<Flashcard> cards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFlashcardsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.rootContainer);
        UiUtils.setupToolbar(this, binding.toolbar, "Flashcards", true);

        materialDao = new MaterialDao(this);
        flashcardDao = new FlashcardDao(this);
        flashcardAiService = new FlashcardAiService(this);

        long materialId = getIntent().getLongExtra(NavExtras.EXTRA_MATERIAL_ID, -1L);
        if (materialId == -1L) {
            finish();
            return;
        }

        binding.cardInnerLayout.setOnClickListener(v -> {
            if (cards.isEmpty()) return;
            showingBack = !showingBack;
            renderCurrentCard();
        });

        binding.btnPrevCard.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showingBack = false;
                renderCurrentCard();
            }
        });

        binding.btnNextCard.setOnClickListener(v -> {
            if (currentIndex < cards.size() - 1) {
                currentIndex++;
                showingBack = false;
                renderCurrentCard();
            }
        });

        binding.btnToggleMastered.setOnClickListener(v -> toggleMastered());
        binding.btnDeleteCard.setOnClickListener(v -> deleteCurrentCard());
        binding.btnGenerateCards.setOnClickListener(v -> showGenerateDialog());
        binding.btnAddCard.setOnClickListener(v -> showAddCardDialog());

        AppExecutors.getInstance().diskIO().execute(() -> {
            Material m = materialDao.getMaterialById(materialId);
            List<Flashcard> saved = flashcardDao.getFlashcardsByMaterial(materialId, false);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (m == null) {
                    finish();
                    return;
                }
                currentMaterial = m;
                setTitle("Flashcards: " + m.getTitle());
                cards.clear();
                cards.addAll(saved);
                currentIndex = 0;
                showingBack = false;
                renderCurrentCard();
            });
        });
    }

    private void renderCurrentCard() {
        if (cards.isEmpty()) {
            binding.tvFlashcardProgress.setText("0 / 0 cards");
            binding.tvCardTopic.setText("");
            binding.tvCardSideLabel.setText("EMPTY");
            binding.tvCardText.setText("No flashcards yet. Tap 'Generate AI Cards' or '+ Manual Card'.");
            binding.btnToggleMastered.setEnabled(false);
            binding.btnDeleteCard.setEnabled(false);
            return;
        }

        if (currentIndex >= cards.size()) currentIndex = cards.size() - 1;
        Flashcard card = cards.get(currentIndex);

        int masteredCount = 0;
        for (Flashcard c : cards) {
            if (c.isMastered()) masteredCount++;
        }

        binding.tvFlashcardProgress.setText("Card " + (currentIndex + 1) + " / " + cards.size()
                + "  •  Mastered: " + masteredCount);
        binding.tvCardTopic.setText("Topic: " + card.getTopic() + (card.isMastered() ? "  [Mastered]" : ""));
        binding.tvCardSideLabel.setText(showingBack ? "BACK (Tap to flip)" : "FRONT (Tap to flip)");
        binding.tvCardText.setText(showingBack ? card.getBackText() : card.getFrontText());
        binding.btnToggleMastered.setEnabled(true);
        binding.btnToggleMastered.setText(card.isMastered() ? "Needs Review" : "Mark Mastered");
        binding.btnDeleteCard.setEnabled(true);
    }

    private void toggleMastered() {
        if (cards.isEmpty()) return;
        Flashcard card = cards.get(currentIndex);
        boolean nextState = !card.isMastered();
        card.setMastered(nextState);
        renderCurrentCard();

        AppExecutors.getInstance().diskIO().execute(() ->
                flashcardDao.setMastered(card.getId(), nextState));
    }

    private void deleteCurrentCard() {
        if (cards.isEmpty()) return;
        Flashcard card = cards.remove(currentIndex);
        if (currentIndex >= cards.size() && currentIndex > 0) {
            currentIndex--;
        }
        showingBack = false;
        renderCurrentCard();

        AppExecutors.getInstance().diskIO().execute(() ->
                flashcardDao.deleteFlashcard(card.getId()));
    }

    private void showGenerateDialog() {
        if (currentMaterial == null) return;
        String[] options = {"Generate 5 Cards (Append)", "Generate 10 Cards (Append)", "Regenerate 10 Cards (Replace All)"};
        new AlertDialog.Builder(this)
                .setTitle("AI Flashcards")
                .setItems(options, (dialog, which) -> {
                    int count = (which == 0) ? 5 : 10;
                    boolean replace = (which == 2);
                    binding.progressFlashcards.setVisibility(View.VISIBLE);
                    binding.btnGenerateCards.setEnabled(false);

                    flashcardAiService.generateFlashcards(currentMaterial, count, replace, new AiCallback<List<Flashcard>>() {
                        @Override
                        public void onSuccess(List<Flashcard> result) {
                            binding.progressFlashcards.setVisibility(View.GONE);
                            binding.btnGenerateCards.setEnabled(true);
                            cards.clear();
                            cards.addAll(result);
                            currentIndex = 0;
                            showingBack = false;
                            renderCurrentCard();
                            Toast.makeText(FlashcardsActivity.this, "Flashcards ready!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            binding.progressFlashcards.setVisibility(View.GONE);
                            binding.btnGenerateCards.setEnabled(true);
                            Toast.makeText(FlashcardsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    private void showAddCardDialog() {
        if (currentMaterial == null) return;
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etTopic = new EditText(this);
        etTopic.setHint("Topic (optional)");
        container.addView(etTopic);

        EditText etFront = new EditText(this);
        etFront.setHint("Front (Question / Term)");
        container.addView(etFront);

        EditText etBack = new EditText(this);
        etBack.setHint("Back (Answer / Definition)");
        container.addView(etBack);

        new AlertDialog.Builder(this)
                .setTitle("Add Manual Flashcard")
                .setView(container)
                .setPositiveButton("Add", (dialog, which) -> {
                    String front = etFront.getText().toString().trim();
                    String back = etBack.getText().toString().trim();
                    String topic = etTopic.getText().toString().trim();
                    if (front.isEmpty() || back.isEmpty()) {
                        Toast.makeText(this, "Front and Back are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Flashcard card = new Flashcard(
                            currentMaterial.getId(),
                            topic.isEmpty() ? "General" : topic,
                            front,
                            back
                    );
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        flashcardDao.insertFlashcard(card);
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            cards.add(card);
                            currentIndex = cards.size() - 1;
                            showingBack = false;
                            renderCurrentCard();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

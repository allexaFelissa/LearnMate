package com.learnmate.app.ui.material;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.learnmate.app.core.db.dao.MaterialDao;
import com.learnmate.app.core.navigation.NavExtras;
import com.learnmate.app.core.utils.AppExecutors;
import com.learnmate.app.core.utils.UiUtils;
import com.learnmate.app.databinding.ActivityMaterialDetailBinding;
import com.learnmate.app.model.Material;
import com.learnmate.app.ui.flashcard.FlashcardsActivity;
import com.learnmate.app.ui.summary.AiSummaryActivity;

public class MaterialDetailActivity extends AppCompatActivity {

    private ActivityMaterialDetailBinding binding;
    private MaterialDao materialDao;
    private long materialId = -1L;
    private Material currentMaterial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMaterialDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.rootContainer);
        UiUtils.setupToolbar(this, binding.toolbar, "Material Detail", true);

        materialDao = new MaterialDao(this);
        materialId = getIntent().getLongExtra(NavExtras.EXTRA_MATERIAL_ID, -1L);
        if (materialId == -1L) {
            Toast.makeText(this, "Invalid material ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnAiSummary.setOnClickListener(v -> {
            Intent intent = new Intent(this, AiSummaryActivity.class);
            intent.putExtra(NavExtras.EXTRA_MATERIAL_ID, materialId);
            startActivity(intent);
        });

        binding.btnFlashcards.setOnClickListener(v -> {
            Intent intent = new Intent(this, FlashcardsActivity.class);
            intent.putExtra(NavExtras.EXTRA_MATERIAL_ID, materialId);
            startActivity(intent);
        });

        // Person 2 integration hooks
        binding.btnTakeQuiz.setOnClickListener(v ->
                Toast.makeText(this, "Hook for Person 2: QuizActivity (materialId=" + materialId + ")", Toast.LENGTH_SHORT).show());

        binding.btnStudyTimer.setOnClickListener(v ->
                Toast.makeText(this, "Hook for Person 2: StudyTimerActivity (materialId=" + materialId + ")", Toast.LENGTH_SHORT).show());

        binding.btnDeleteMaterial.setOnClickListener(v -> confirmDelete());

        loadMaterial();
    }

    private void loadMaterial() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            materialDao.touchLastOpened(materialId);
            Material m = materialDao.getMaterialById(materialId);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (m == null) {
                    Toast.makeText(this, "Material not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                currentMaterial = m;
                binding.tvDetailTitle.setText(m.getTitle());
                String subject = m.getSubjectName() != null ? m.getSubjectName() : "General";
                int charLen = m.getExtractedText() != null ? m.getExtractedText().length() : 0;
                binding.tvDetailMeta.setText(subject + " • " + m.getPageCount() + " pages • " + charLen + " chars extracted");

                String preview = m.getExtractedText() != null ? m.getExtractedText() : "";
                if (preview.length() > 1500) {
                    preview = preview.substring(0, 1500) + "\n\n... [truncated preview]";
                }
                binding.tvExtractedPreview.setText(preview);
            });
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Material?")
                .setMessage("This will permanently delete this PDF along with its cached summaries and flashcards.")
                .setPositiveButton("Delete", (dialog, which) ->
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            materialDao.deleteMaterial(materialId);
                            AppExecutors.getInstance().mainThread().execute(() -> {
                                Toast.makeText(this, "Material deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}

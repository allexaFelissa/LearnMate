package com.learnmate.app.ui.summary;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.learnmate.app.core.ai.AiCallback;
import com.learnmate.app.core.ai.GeminiClient;
import com.learnmate.app.core.ai.SummaryAiService;
import com.learnmate.app.core.db.dao.MaterialDao;
import com.learnmate.app.core.navigation.NavExtras;
import com.learnmate.app.core.utils.AppExecutors;
import com.learnmate.app.core.utils.UiUtils;
import com.learnmate.app.databinding.ActivityAiSummaryBinding;
import com.learnmate.app.model.Material;
import com.learnmate.app.model.Summary;

public class AiSummaryActivity extends AppCompatActivity {

    private ActivityAiSummaryBinding binding;
    private MaterialDao materialDao;
    private SummaryAiService summaryAiService;

    private Material currentMaterial;
    private String currentStyle = Summary.STYLE_CONCISE;
    private String renderedSummaryText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.rootContainer);
        UiUtils.setupToolbar(this, binding.toolbar, "AI Summary", true);

        materialDao = new MaterialDao(this);
        summaryAiService = new SummaryAiService(this);

        long materialId = getIntent().getLongExtra(NavExtras.EXTRA_MATERIAL_ID, -1L);
        if (materialId == -1L) {
            finish();
            return;
        }

        binding.btnStyleConcise.setOnClickListener(v -> switchStyle(Summary.STYLE_CONCISE));
        binding.btnStyleDetailed.setOnClickListener(v -> switchStyle(Summary.STYLE_DETAILED));
        binding.btnStyleExam.setOnClickListener(v -> switchStyle(Summary.STYLE_EXAM_CHEAT_SHEET));
        binding.btnRegenerateSummary.setOnClickListener(v -> loadSummary(true));
        binding.btnCopySummary.setOnClickListener(v -> copyToClipboard());

        AppExecutors.getInstance().diskIO().execute(() -> {
            Material m = materialDao.getMaterialById(materialId);
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (m == null) {
                    finish();
                    return;
                }
                currentMaterial = m;
                binding.tvSummaryHeader.setText(m.getTitle());
                loadSummary(false);
            });
        });
    }

    private void switchStyle(String style) {
        currentStyle = style;
        loadSummary(false);
    }

    private void loadSummary(boolean forceRegenerate) {
        if (currentMaterial == null) return;
        binding.progressSummary.setVisibility(View.VISIBLE);
        binding.tvSummaryStatus.setText(forceRegenerate
                ? "Regenerating " + currentStyle + " summary via Gemini..."
                : "Loading " + currentStyle + " summary...");

        summaryAiService.getOrGenerateSummary(currentMaterial, currentStyle, forceRegenerate, new AiCallback<Summary>() {
            @Override
            public void onSuccess(Summary summary) {
                binding.progressSummary.setVisibility(View.GONE);
                String modelInfo = GeminiClient.getInstance().getLastUsedModel();
                String suffix = modelInfo.isEmpty() ? "Loaded from SQLite" : "Saved via " + modelInfo;
                binding.tvSummaryStatus.setText("Style: " + summary.getStyle() + " (" + suffix + ")");
                renderedSummaryText = formatSummary(summary);
                binding.tvSummaryContent.setText(renderedSummaryText);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressSummary.setVisibility(View.GONE);
                binding.tvSummaryStatus.setText("Error: " + errorMessage);
                Toast.makeText(AiSummaryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatSummary(Summary summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("OVERVIEW\n").append(summary.getOverview()).append("\n\n");

        sb.append("KEY POINTS\n");
        for (String pt : summary.getKeyPoints()) {
            sb.append("• ").append(pt).append("\n");
        }

        if (!summary.getKeyTerms().isEmpty()) {
            sb.append("\nKEY TERMS\n");
            for (Summary.KeyTerm kt : summary.getKeyTerms()) {
                sb.append("• ").append(kt.getTerm()).append(": ").append(kt.getDefinition()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private void copyToClipboard() {
        if (renderedSummaryText.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("LearnMate Summary", renderedSummaryText));
            Toast.makeText(this, "Summary copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}

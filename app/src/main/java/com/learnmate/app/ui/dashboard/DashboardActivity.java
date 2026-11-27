package com.learnmate.app.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.learnmate.app.core.db.dao.FlashcardDao;
import com.learnmate.app.core.db.dao.MaterialDao;
import com.learnmate.app.core.navigation.NavExtras;
import com.learnmate.app.core.utils.AppExecutors;
import com.learnmate.app.core.utils.UiUtils;
import com.learnmate.app.databinding.ActivityDashboardBinding;
import com.learnmate.app.model.Material;
import com.learnmate.app.ui.material.MaterialAdapter;
import com.learnmate.app.ui.material.MaterialDetailActivity;
import com.learnmate.app.ui.material.MyMaterialsActivity;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;
    private MaterialDao materialDao;
    private FlashcardDao flashcardDao;
    private MaterialAdapter recentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.rootContainer);
        UiUtils.setupToolbar(this, binding.toolbar, "LearnMate Dashboard", false);

        materialDao = new MaterialDao(this);
        flashcardDao = new FlashcardDao(this);

        recentAdapter = new MaterialAdapter(material -> {
            Intent intent = new Intent(this, MaterialDetailActivity.class);
            intent.putExtra(NavExtras.EXTRA_MATERIAL_ID, material.getId());
            startActivity(intent);
        });
        binding.rvRecentMaterials.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentMaterials.setAdapter(recentAdapter);

        binding.btnMyMaterials.setOnClickListener(v ->
                startActivity(new Intent(this, MyMaterialsActivity.class)));

        // Person 2 integration hooks — replace with live Intents once Person 2 merges
        binding.btnProgressPlaceholder.setOnClickListener(v ->
                Toast.makeText(this, "Hook for Person 2: ProgressDashboardActivity", Toast.LENGTH_SHORT).show());
        binding.btnReadinessPlaceholder.setOnClickListener(v ->
                Toast.makeText(this, "Hook for Person 2: StudyReadinessActivity", Toast.LENGTH_SHORT).show());
        binding.btnTimerPlaceholder.setOnClickListener(v ->
                Toast.makeText(this, "Hook for Person 2: StudyTimerActivity", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            int materialCount = materialDao.getMaterialCount();
            int masteredCards = flashcardDao.getTotalMasteredCount();
            int totalCards = flashcardDao.getTotalFlashcardCount();
            List<Material> recent = materialDao.getRecentMaterials(5);

            AppExecutors.getInstance().mainThread().execute(() -> {
                binding.tvStatsSummary.setText("Materials: " + materialCount
                        + " | Mastered Flashcards: " + masteredCards + "/" + totalCards);
                recentAdapter.submitList(recent);
                binding.tvRecentEmpty.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }
}

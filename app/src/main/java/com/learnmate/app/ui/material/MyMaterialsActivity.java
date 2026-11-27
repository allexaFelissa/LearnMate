package com.learnmate.app.ui.material;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.learnmate.app.core.ai.AiCallback;
import com.learnmate.app.core.db.dao.MaterialDao;
import com.learnmate.app.core.db.dao.SubjectDao;
import com.learnmate.app.core.navigation.NavExtras;
import com.learnmate.app.core.utils.AppExecutors;
import com.learnmate.app.core.utils.PdfTextExtractor;
import com.learnmate.app.core.utils.UiUtils;
import com.learnmate.app.databinding.ActivityMyMaterialsBinding;
import com.learnmate.app.model.Material;
import com.learnmate.app.model.Subject;

import java.util.ArrayList;
import java.util.List;

public class MyMaterialsActivity extends AppCompatActivity {

    private ActivityMyMaterialsBinding binding;
    private SubjectDao subjectDao;
    private MaterialDao materialDao;
    private MaterialAdapter adapter;

    private final List<Subject> subjects = new ArrayList<>();
    private Long selectedSubjectFilterId = null;

    private final ActivityResultLauncher<String[]> pdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onPdfSelected);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyMaterialsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.rootContainer);
        UiUtils.setupToolbar(this, binding.toolbar, "My Materials", true);

        subjectDao = new SubjectDao(this);
        materialDao = new MaterialDao(this);

        adapter = new MaterialAdapter(material -> {
            Intent intent = new Intent(this, MaterialDetailActivity.class);
            intent.putExtra(NavExtras.EXTRA_MATERIAL_ID, material.getId());
            startActivity(intent);
        });
        binding.rvMaterials.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMaterials.setAdapter(adapter);

        binding.btnUploadPdf.setOnClickListener(v ->
                pdfPickerLauncher.launch(new String[]{"application/pdf"}));

        binding.btnAddSubject.setOnClickListener(v -> showAddSubjectDialog());

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadMaterials();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.spinnerSubjectFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedSubjectFilterId = null;
                } else if (position - 1 < subjects.size()) {
                    selectedSubjectFilterId = subjects.get(position - 1).getId();
                }
                loadMaterials();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubjectsAndMaterials();
    }

    private void loadSubjectsAndMaterials() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Subject> loaded = subjectDao.getAllSubjects();
            AppExecutors.getInstance().mainThread().execute(() -> {
                subjects.clear();
                subjects.addAll(loaded);

                List<String> filterLabels = new ArrayList<>();
                filterLabels.add("All Subjects");
                for (Subject s : subjects) {
                    filterLabels.add(s.getName());
                }
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_dropdown_item, filterLabels);
                binding.spinnerSubjectFilter.setAdapter(spinnerAdapter);
                loadMaterials();
            });
        });
    }

    private void loadMaterials() {
        String query = binding.etSearch.getText().toString();
        Long subjectId = selectedSubjectFilterId;
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Material> list = materialDao.filterMaterials(subjectId, query);
            AppExecutors.getInstance().mainThread().execute(() -> {
                adapter.submitList(list);
                binding.tvEmptyMaterials.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void onPdfSelected(Uri uri) {
        if (uri == null) return;
        binding.progressUpload.setVisibility(View.VISIBLE);
        binding.btnUploadPdf.setEnabled(false);

        PdfTextExtractor.extractFromUri(this, uri, new AiCallback<PdfTextExtractor.PdfExtractResult>() {
            @Override
            public void onSuccess(PdfTextExtractor.PdfExtractResult result) {
                binding.progressUpload.setVisibility(View.GONE);
                binding.btnUploadPdf.setEnabled(true);
                showSaveMaterialDialog(result);
            }

            @Override
            public void onError(String errorMessage) {
                binding.progressUpload.setVisibility(View.GONE);
                binding.btnUploadPdf.setEnabled(true);
                Toast.makeText(MyMaterialsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSaveMaterialDialog(PdfTextExtractor.PdfExtractResult result) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Material Title");
        etTitle.setText(result.suggestedTitle);
        container.addView(etTitle);

        Spinner spSubject = new Spinner(this);
        ArrayAdapter<Subject> subjectAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, subjects);
        spSubject.setAdapter(subjectAdapter);
        container.addView(spSubject);

        new AlertDialog.Builder(this)
                .setTitle("Save PDF Material (" + result.pageCount + " pages)")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) title = result.suggestedTitle;
                    Subject chosen = (Subject) spSubject.getSelectedItem();
                    long subjectId = chosen != null ? chosen.getId() : 1L;

                    Material material = new Material();
                    material.setSubjectId(subjectId);
                    material.setTitle(title);
                    material.setFilePath(result.savedFilePath);
                    material.setPageCount(result.pageCount);
                    material.setExtractedText(result.extractedText);

                    AppExecutors.getInstance().diskIO().execute(() -> {
                        materialDao.insertMaterial(material);
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            Toast.makeText(this, "Material saved!", Toast.LENGTH_SHORT).show();
                            loadMaterials();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddSubjectDialog() {
        EditText etName = new EditText(this);
        etName.setHint("Subject name (e.g. Biology)");

        new AlertDialog.Builder(this)
                .setTitle("New Subject")
                .setView(etName)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        subjectDao.insertSubject(name, "#3B82F6");
                        AppExecutors.getInstance().mainThread().execute(this::loadSubjectsAndMaterials);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

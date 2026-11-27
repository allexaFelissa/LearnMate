package com.learnmate.app.core.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.learnmate.app.core.db.DbContract.MaterialsTable;
import com.learnmate.app.core.db.DbContract.SubjectsTable;
import com.learnmate.app.core.db.LearnMateDbHelper;
import com.learnmate.app.model.Material;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MaterialDao {
    private final LearnMateDbHelper dbHelper;

    private static final String BASE_SELECT =
            "SELECT m." + MaterialsTable._ID + ", "
                    + "m." + MaterialsTable.COL_SUBJECT_ID + ", "
                    + "s." + SubjectsTable.COL_NAME + " AS subject_name, "
                    + "m." + MaterialsTable.COL_TITLE + ", "
                    + "m." + MaterialsTable.COL_FILE_PATH + ", "
                    + "m." + MaterialsTable.COL_PAGE_COUNT + ", "
                    + "m." + MaterialsTable.COL_EXTRACTED_TEXT + ", "
                    + "m." + MaterialsTable.COL_CREATED_AT + ", "
                    + "m." + MaterialsTable.COL_LAST_OPENED_AT + " "
                    + "FROM " + MaterialsTable.TABLE_NAME + " m "
                    + "LEFT JOIN " + SubjectsTable.TABLE_NAME + " s "
                    + "ON m." + MaterialsTable.COL_SUBJECT_ID + " = s." + SubjectsTable._ID + " ";

    public MaterialDao(Context context) {
        this.dbHelper = LearnMateDbHelper.getInstance(context);
    }

    public long insertMaterial(Material material) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put(MaterialsTable.COL_SUBJECT_ID, material.getSubjectId());
        cv.put(MaterialsTable.COL_TITLE, material.getTitle());
        cv.put(MaterialsTable.COL_FILE_PATH, material.getFilePath());
        cv.put(MaterialsTable.COL_PAGE_COUNT, material.getPageCount());
        cv.put(MaterialsTable.COL_EXTRACTED_TEXT, material.getExtractedText() != null ? material.getExtractedText() : "");
        cv.put(MaterialsTable.COL_CREATED_AT, material.getCreatedAt() > 0 ? material.getCreatedAt() : now);
        cv.put(MaterialsTable.COL_LAST_OPENED_AT, material.getLastOpenedAt() > 0 ? material.getLastOpenedAt() : now);
        long id = db.insert(MaterialsTable.TABLE_NAME, null, cv);
        material.setId(id);
        return id;
    }

    public List<Material> getAllMaterials() {
        return queryMaterials(BASE_SELECT + "ORDER BY m." + MaterialsTable.COL_CREATED_AT + " DESC", null);
    }

    public List<Material> getRecentMaterials(int limit) {
        return queryMaterials(
                BASE_SELECT + "ORDER BY m." + MaterialsTable.COL_LAST_OPENED_AT + " DESC LIMIT ?",
                new String[]{String.valueOf(limit)}
        );
    }

    public List<Material> filterMaterials(Long subjectId, String searchQuery) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<String> args = new ArrayList<>();
        boolean hasWhere = false;

        if (subjectId != null && subjectId > 0) {
            sql.append("WHERE m.").append(MaterialsTable.COL_SUBJECT_ID).append(" = ? ");
            args.add(String.valueOf(subjectId));
            hasWhere = true;
        }
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            sql.append(hasWhere ? "AND " : "WHERE ");
            sql.append("m.").append(MaterialsTable.COL_TITLE).append(" LIKE ? ");
            args.add("%" + searchQuery.trim() + "%");
        }
        sql.append("ORDER BY m.").append(MaterialsTable.COL_CREATED_AT).append(" DESC");

        return queryMaterials(sql.toString(), args.toArray(new String[0]));
    }

    public Material getMaterialById(long id) {
        List<Material> results = queryMaterials(
                BASE_SELECT + "WHERE m." + MaterialsTable._ID + " = ?",
                new String[]{String.valueOf(id)}
        );
        return results.isEmpty() ? null : results.get(0);
    }

    public void touchLastOpened(long materialId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MaterialsTable.COL_LAST_OPENED_AT, System.currentTimeMillis());
        db.update(MaterialsTable.TABLE_NAME, cv, MaterialsTable._ID + " = ?", new String[]{String.valueOf(materialId)});
    }

    public int updateTitle(long materialId, String newTitle) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MaterialsTable.COL_TITLE, newTitle.trim());
        return db.update(MaterialsTable.TABLE_NAME, cv, MaterialsTable._ID + " = ?", new String[]{String.valueOf(materialId)});
    }

    public int deleteMaterial(long materialId) {
        Material existing = getMaterialById(materialId);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete(MaterialsTable.TABLE_NAME, MaterialsTable._ID + " = ?", new String[]{String.valueOf(materialId)});
        if (deleted > 0 && existing != null && existing.getFilePath() != null) {
            File pdfFile = new File(existing.getFilePath());
            if (pdfFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                pdfFile.delete();
            }
        }
        return deleted;
    }

    public int getMaterialCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + MaterialsTable.TABLE_NAME, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private List<Material> queryMaterials(String sql, String[] args) {
        List<Material> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                Material m = new Material();
                m.setId(c.getLong(c.getColumnIndexOrThrow(MaterialsTable._ID)));
                m.setSubjectId(c.getLong(c.getColumnIndexOrThrow(MaterialsTable.COL_SUBJECT_ID)));
                m.setSubjectName(c.getString(c.getColumnIndexOrThrow("subject_name")));
                m.setTitle(c.getString(c.getColumnIndexOrThrow(MaterialsTable.COL_TITLE)));
                m.setFilePath(c.getString(c.getColumnIndexOrThrow(MaterialsTable.COL_FILE_PATH)));
                m.setPageCount(c.getInt(c.getColumnIndexOrThrow(MaterialsTable.COL_PAGE_COUNT)));
                m.setExtractedText(c.getString(c.getColumnIndexOrThrow(MaterialsTable.COL_EXTRACTED_TEXT)));
                m.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(MaterialsTable.COL_CREATED_AT)));
                m.setLastOpenedAt(c.getLong(c.getColumnIndexOrThrow(MaterialsTable.COL_LAST_OPENED_AT)));
                list.add(m);
            }
        }
        return list;
    }
}

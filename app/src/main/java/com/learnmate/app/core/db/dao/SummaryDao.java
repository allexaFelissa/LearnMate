package com.learnmate.app.core.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.learnmate.app.core.db.DbContract.SummariesTable;
import com.learnmate.app.core.db.LearnMateDbHelper;
import com.learnmate.app.model.Summary;

import java.lang.reflect.Type;
import java.util.List;

public class SummaryDao {
    private final LearnMateDbHelper dbHelper;
    private final Gson gson = new Gson();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    private static final Type KEY_TERM_LIST_TYPE = new TypeToken<List<Summary.KeyTerm>>() {}.getType();

    public SummaryDao(Context context) {
        this.dbHelper = LearnMateDbHelper.getInstance(context);
    }

    public long upsertSummary(Summary summary) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SummariesTable.COL_MATERIAL_ID, summary.getMaterialId());
        cv.put(SummariesTable.COL_STYLE, summary.getStyle());
        cv.put(SummariesTable.COL_OVERVIEW, summary.getOverview() != null ? summary.getOverview() : "");
        cv.put(SummariesTable.COL_KEY_POINTS_JSON, gson.toJson(summary.getKeyPoints()));
        cv.put(SummariesTable.COL_KEY_TERMS_JSON, gson.toJson(summary.getKeyTerms()));
        cv.put(SummariesTable.COL_GENERATED_AT, System.currentTimeMillis());

        long id = db.insertWithOnConflict(SummariesTable.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        summary.setId(id);
        return id;
    }

    public Summary getSummary(long materialId, String style) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(
                SummariesTable.TABLE_NAME,
                null,
                SummariesTable.COL_MATERIAL_ID + " = ? AND " + SummariesTable.COL_STYLE + " = ?",
                new String[]{String.valueOf(materialId), style},
                null,
                null,
                null)) {
            if (c.moveToFirst()) {
                Summary s = new Summary();
                s.setId(c.getLong(c.getColumnIndexOrThrow(SummariesTable._ID)));
                s.setMaterialId(c.getLong(c.getColumnIndexOrThrow(SummariesTable.COL_MATERIAL_ID)));
                s.setStyle(c.getString(c.getColumnIndexOrThrow(SummariesTable.COL_STYLE)));
                s.setOverview(c.getString(c.getColumnIndexOrThrow(SummariesTable.COL_OVERVIEW)));
                String kpJson = c.getString(c.getColumnIndexOrThrow(SummariesTable.COL_KEY_POINTS_JSON));
                String ktJson = c.getString(c.getColumnIndexOrThrow(SummariesTable.COL_KEY_TERMS_JSON));
                s.setKeyPoints(gson.fromJson(kpJson, STRING_LIST_TYPE));
                s.setKeyTerms(gson.fromJson(ktJson, KEY_TERM_LIST_TYPE));
                s.setGeneratedAt(c.getLong(c.getColumnIndexOrThrow(SummariesTable.COL_GENERATED_AT)));
                return s;
            }
        }
        return null;
    }

    public int deleteSummariesForMaterial(long materialId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(SummariesTable.TABLE_NAME, SummariesTable.COL_MATERIAL_ID + " = ?", new String[]{String.valueOf(materialId)});
    }
}

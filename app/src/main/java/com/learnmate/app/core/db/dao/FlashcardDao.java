package com.learnmate.app.core.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.learnmate.app.core.db.DbContract.FlashcardsTable;
import com.learnmate.app.core.db.LearnMateDbHelper;
import com.learnmate.app.model.Flashcard;

import java.util.ArrayList;
import java.util.List;

public class FlashcardDao {
    private final LearnMateDbHelper dbHelper;

    public FlashcardDao(Context context) {
        this.dbHelper = LearnMateDbHelper.getInstance(context);
    }

    public long insertFlashcard(Flashcard card) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = toContentValues(card);
        long id = db.insert(FlashcardsTable.TABLE_NAME, null, cv);
        card.setId(id);
        return id;
    }

    public void bulkInsert(List<Flashcard> cards) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Flashcard card : cards) {
                long id = db.insert(FlashcardsTable.TABLE_NAME, null, toContentValues(card));
                card.setId(id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Flashcard> getFlashcardsByMaterial(long materialId, boolean unmasteredOnly) {
        List<Flashcard> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = FlashcardsTable.COL_MATERIAL_ID + " = ?"
                + (unmasteredOnly ? " AND " + FlashcardsTable.COL_IS_MASTERED + " = 0" : "");

        try (Cursor c = db.query(
                FlashcardsTable.TABLE_NAME,
                null,
                selection,
                new String[]{String.valueOf(materialId)},
                null,
                null,
                FlashcardsTable._ID + " ASC")) {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        }
        return list;
    }

    public int setMastered(long flashcardId, boolean mastered) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(FlashcardsTable.COL_IS_MASTERED, mastered ? 1 : 0);
        return db.update(FlashcardsTable.TABLE_NAME, cv, FlashcardsTable._ID + " = ?", new String[]{String.valueOf(flashcardId)});
    }

    public int deleteFlashcard(long flashcardId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(FlashcardsTable.TABLE_NAME, FlashcardsTable._ID + " = ?", new String[]{String.valueOf(flashcardId)});
    }

    public int deleteAllForMaterial(long materialId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(FlashcardsTable.TABLE_NAME, FlashcardsTable.COL_MATERIAL_ID + " = ?", new String[]{String.valueOf(materialId)});
    }

    public int getTotalMasteredCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + FlashcardsTable.TABLE_NAME + " WHERE " + FlashcardsTable.COL_IS_MASTERED + " = 1",
                null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int getTotalFlashcardCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + FlashcardsTable.TABLE_NAME, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private ContentValues toContentValues(Flashcard card) {
        ContentValues cv = new ContentValues();
        cv.put(FlashcardsTable.COL_MATERIAL_ID, card.getMaterialId());
        cv.put(FlashcardsTable.COL_TOPIC, card.getTopic() != null ? card.getTopic() : "General");
        cv.put(FlashcardsTable.COL_FRONT_TEXT, card.getFrontText());
        cv.put(FlashcardsTable.COL_BACK_TEXT, card.getBackText());
        cv.put(FlashcardsTable.COL_IS_MASTERED, card.isMastered() ? 1 : 0);
        cv.put(FlashcardsTable.COL_CREATED_AT, card.getCreatedAt() > 0 ? card.getCreatedAt() : System.currentTimeMillis());
        return cv;
    }

    private Flashcard fromCursor(Cursor c) {
        Flashcard f = new Flashcard();
        f.setId(c.getLong(c.getColumnIndexOrThrow(FlashcardsTable._ID)));
        f.setMaterialId(c.getLong(c.getColumnIndexOrThrow(FlashcardsTable.COL_MATERIAL_ID)));
        f.setTopic(c.getString(c.getColumnIndexOrThrow(FlashcardsTable.COL_TOPIC)));
        f.setFrontText(c.getString(c.getColumnIndexOrThrow(FlashcardsTable.COL_FRONT_TEXT)));
        f.setBackText(c.getString(c.getColumnIndexOrThrow(FlashcardsTable.COL_BACK_TEXT)));
        f.setMastered(c.getInt(c.getColumnIndexOrThrow(FlashcardsTable.COL_IS_MASTERED)) == 1);
        f.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(FlashcardsTable.COL_CREATED_AT)));
        return f;
    }
}

package com.learnmate.app.core.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.learnmate.app.core.db.DbContract.SubjectsTable;
import com.learnmate.app.core.db.LearnMateDbHelper;
import com.learnmate.app.model.Subject;

import java.util.ArrayList;
import java.util.List;

public class SubjectDao {
    private final LearnMateDbHelper dbHelper;

    public SubjectDao(Context context) {
        this.dbHelper = LearnMateDbHelper.getInstance(context);
    }

    public long insertSubject(String name, String colorHex) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SubjectsTable.COL_NAME, name.trim());
        cv.put(SubjectsTable.COL_COLOR_HEX, colorHex != null ? colorHex : "#3B82F6");
        cv.put(SubjectsTable.COL_CREATED_AT, System.currentTimeMillis());
        return db.insertWithOnConflict(SubjectsTable.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<Subject> getAllSubjects() {
        List<Subject> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(
                SubjectsTable.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                SubjectsTable._ID + " ASC")) {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        }
        return list;
    }

    public Subject getSubjectById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor c = db.query(
                SubjectsTable.TABLE_NAME,
                null,
                SubjectsTable._ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null)) {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        }
        return null;
    }

    private Subject fromCursor(Cursor c) {
        return new Subject(
                c.getLong(c.getColumnIndexOrThrow(SubjectsTable._ID)),
                c.getString(c.getColumnIndexOrThrow(SubjectsTable.COL_NAME)),
                c.getString(c.getColumnIndexOrThrow(SubjectsTable.COL_COLOR_HEX)),
                c.getLong(c.getColumnIndexOrThrow(SubjectsTable.COL_CREATED_AT))
        );
    }
}

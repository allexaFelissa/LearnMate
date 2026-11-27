package com.learnmate.app.core.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.learnmate.app.core.db.DbContract.FlashcardsTable;
import com.learnmate.app.core.db.DbContract.MaterialsTable;
import com.learnmate.app.core.db.DbContract.QuizAttemptsTable;
import com.learnmate.app.core.db.DbContract.ReadinessSnapshotsTable;
import com.learnmate.app.core.db.DbContract.StudySessionsTable;
import com.learnmate.app.core.db.DbContract.SubjectsTable;
import com.learnmate.app.core.db.DbContract.SummariesTable;
import com.learnmate.app.core.db.DbContract.TopicPerformanceTable;

/**
 * Singleton SQLiteOpenHelper managing all tables for Person 1 and Person 2.
 */
public class LearnMateDbHelper extends SQLiteOpenHelper {

    private static volatile LearnMateDbHelper instance;

    public static LearnMateDbHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (LearnMateDbHelper.class) {
                if (instance == null) {
                    instance = new LearnMateDbHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private LearnMateDbHelper(Context context) {
        super(context, DbContract.DATABASE_NAME, null, DbContract.DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Subjects (Person 1)
        db.execSQL("CREATE TABLE " + SubjectsTable.TABLE_NAME + " ("
                + SubjectsTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SubjectsTable.COL_NAME + " TEXT NOT NULL UNIQUE, "
                + SubjectsTable.COL_COLOR_HEX + " TEXT NOT NULL DEFAULT '#3B82F6', "
                + SubjectsTable.COL_CREATED_AT + " INTEGER NOT NULL"
                + ");");

        // 2. Materials (Person 1)
        db.execSQL("CREATE TABLE " + MaterialsTable.TABLE_NAME + " ("
                + MaterialsTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MaterialsTable.COL_SUBJECT_ID + " INTEGER NOT NULL, "
                + MaterialsTable.COL_TITLE + " TEXT NOT NULL, "
                + MaterialsTable.COL_FILE_PATH + " TEXT NOT NULL, "
                + MaterialsTable.COL_PAGE_COUNT + " INTEGER NOT NULL DEFAULT 0, "
                + MaterialsTable.COL_EXTRACTED_TEXT + " TEXT NOT NULL DEFAULT '', "
                + MaterialsTable.COL_CREATED_AT + " INTEGER NOT NULL, "
                + MaterialsTable.COL_LAST_OPENED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY(" + MaterialsTable.COL_SUBJECT_ID + ") REFERENCES "
                + SubjectsTable.TABLE_NAME + "(" + SubjectsTable._ID + ") ON DELETE RESTRICT"
                + ");");

        // 3. Summaries (Person 1)
        db.execSQL("CREATE TABLE " + SummariesTable.TABLE_NAME + " ("
                + SummariesTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SummariesTable.COL_MATERIAL_ID + " INTEGER NOT NULL, "
                + SummariesTable.COL_STYLE + " TEXT NOT NULL, "
                + SummariesTable.COL_OVERVIEW + " TEXT NOT NULL, "
                + SummariesTable.COL_KEY_POINTS_JSON + " TEXT NOT NULL DEFAULT '[]', "
                + SummariesTable.COL_KEY_TERMS_JSON + " TEXT NOT NULL DEFAULT '[]', "
                + SummariesTable.COL_GENERATED_AT + " INTEGER NOT NULL, "
                + "UNIQUE(" + SummariesTable.COL_MATERIAL_ID + ", " + SummariesTable.COL_STYLE + "), "
                + "FOREIGN KEY(" + SummariesTable.COL_MATERIAL_ID + ") REFERENCES "
                + MaterialsTable.TABLE_NAME + "(" + MaterialsTable._ID + ") ON DELETE CASCADE"
                + ");");

        // 4. Flashcards (Person 1)
        db.execSQL("CREATE TABLE " + FlashcardsTable.TABLE_NAME + " ("
                + FlashcardsTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FlashcardsTable.COL_MATERIAL_ID + " INTEGER NOT NULL, "
                + FlashcardsTable.COL_TOPIC + " TEXT NOT NULL DEFAULT 'General', "
                + FlashcardsTable.COL_FRONT_TEXT + " TEXT NOT NULL, "
                + FlashcardsTable.COL_BACK_TEXT + " TEXT NOT NULL, "
                + FlashcardsTable.COL_IS_MASTERED + " INTEGER NOT NULL DEFAULT 0, "
                + FlashcardsTable.COL_CREATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY(" + FlashcardsTable.COL_MATERIAL_ID + ") REFERENCES "
                + MaterialsTable.TABLE_NAME + "(" + MaterialsTable._ID + ") ON DELETE CASCADE"
                + ");");

        // 5. Quiz Attempts (Person 2)
        db.execSQL("CREATE TABLE " + QuizAttemptsTable.TABLE_NAME + " ("
                + QuizAttemptsTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + QuizAttemptsTable.COL_MATERIAL_ID + " INTEGER NOT NULL, "
                + QuizAttemptsTable.COL_SCORE + " INTEGER NOT NULL, "
                + QuizAttemptsTable.COL_TOTAL_QUESTIONS + " INTEGER NOT NULL, "
                + QuizAttemptsTable.COL_PERCENTAGE + " REAL NOT NULL, "
                + QuizAttemptsTable.COL_TAKEN_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY(" + QuizAttemptsTable.COL_MATERIAL_ID + ") REFERENCES "
                + MaterialsTable.TABLE_NAME + "(" + MaterialsTable._ID + ") ON DELETE CASCADE"
                + ");");

        // 6. Study Sessions (Person 2)
        db.execSQL("CREATE TABLE " + StudySessionsTable.TABLE_NAME + " ("
                + StudySessionsTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + StudySessionsTable.COL_MATERIAL_ID + " INTEGER, "
                + StudySessionsTable.COL_SUBJECT_ID + " INTEGER, "
                + StudySessionsTable.COL_DURATION_SECONDS + " INTEGER NOT NULL, "
                + StudySessionsTable.COL_COMPLETED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY(" + StudySessionsTable.COL_MATERIAL_ID + ") REFERENCES "
                + MaterialsTable.TABLE_NAME + "(" + MaterialsTable._ID + ") ON DELETE SET NULL, "
                + "FOREIGN KEY(" + StudySessionsTable.COL_SUBJECT_ID + ") REFERENCES "
                + SubjectsTable.TABLE_NAME + "(" + SubjectsTable._ID + ") ON DELETE SET NULL"
                + ");");

        // 7. Topic Performance (Person 2)
        db.execSQL("CREATE TABLE " + TopicPerformanceTable.TABLE_NAME + " ("
                + TopicPerformanceTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TopicPerformanceTable.COL_SUBJECT_ID + " INTEGER NOT NULL, "
                + TopicPerformanceTable.COL_TOPIC_NAME + " TEXT NOT NULL, "
                + TopicPerformanceTable.COL_CORRECT_COUNT + " INTEGER NOT NULL DEFAULT 0, "
                + TopicPerformanceTable.COL_TOTAL_COUNT + " INTEGER NOT NULL DEFAULT 0, "
                + TopicPerformanceTable.COL_MASTERY_SCORE + " REAL NOT NULL DEFAULT 0.0, "
                + TopicPerformanceTable.COL_UPDATED_AT + " INTEGER NOT NULL, "
                + "UNIQUE(" + TopicPerformanceTable.COL_SUBJECT_ID + ", " + TopicPerformanceTable.COL_TOPIC_NAME + "), "
                + "FOREIGN KEY(" + TopicPerformanceTable.COL_SUBJECT_ID + ") REFERENCES "
                + SubjectsTable.TABLE_NAME + "(" + SubjectsTable._ID + ") ON DELETE CASCADE"
                + ");");

        // 8. Readiness Snapshots (Person 2)
        db.execSQL("CREATE TABLE " + ReadinessSnapshotsTable.TABLE_NAME + " ("
                + ReadinessSnapshotsTable._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ReadinessSnapshotsTable.COL_SUBJECT_ID + " INTEGER, "
                + ReadinessSnapshotsTable.COL_READINESS_SCORE + " REAL NOT NULL, "
                + ReadinessSnapshotsTable.COL_NOTES + " TEXT, "
                + ReadinessSnapshotsTable.COL_CALCULATED_AT + " INTEGER NOT NULL, "
                + "FOREIGN KEY(" + ReadinessSnapshotsTable.COL_SUBJECT_ID + ") REFERENCES "
                + SubjectsTable.TABLE_NAME + "(" + SubjectsTable._ID + ") ON DELETE SET NULL"
                + ");");

        // Indexes for faster lookups
        db.execSQL("CREATE INDEX idx_materials_subject ON " + MaterialsTable.TABLE_NAME + "(" + MaterialsTable.COL_SUBJECT_ID + ");");
        db.execSQL("CREATE INDEX idx_summaries_material ON " + SummariesTable.TABLE_NAME + "(" + SummariesTable.COL_MATERIAL_ID + ");");
        db.execSQL("CREATE INDEX idx_flashcards_material ON " + FlashcardsTable.TABLE_NAME + "(" + FlashcardsTable.COL_MATERIAL_ID + ");");
        db.execSQL("CREATE INDEX idx_quiz_attempts_material ON " + QuizAttemptsTable.TABLE_NAME + "(" + QuizAttemptsTable.COL_MATERIAL_ID + ");");

        seedDefaultSubjects(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle future schema migrations cleanly here
    }

    private void seedDefaultSubjects(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        String[][] defaults = {
                {"General", "#64748B"},
                {"Computer Science", "#3B82F6"},
                {"Mathematics", "#8B5CF6"},
                {"Science", "#10B981"},
                {"History", "#F59E0B"}
        };

        for (String[] entry : defaults) {
            ContentValues cv = new ContentValues();
            cv.put(SubjectsTable.COL_NAME, entry[0]);
            cv.put(SubjectsTable.COL_COLOR_HEX, entry[1]);
            cv.put(SubjectsTable.COL_CREATED_AT, now);
            db.insertWithOnConflict(SubjectsTable.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }
}

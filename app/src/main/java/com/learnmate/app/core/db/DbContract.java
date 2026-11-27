package com.learnmate.app.core.db;

import android.provider.BaseColumns;

/**
 * Shared SQLite schema contract for both Person 1 (Material & AI) and Person 2 (Quiz & Progress).
 */
public final class DbContract {
    private DbContract() {}

    public static final String DATABASE_NAME = "learnmate.db";
    public static final int DATABASE_VERSION = 1;

    // ==================== PERSON 1 TABLES ====================

    public static final class SubjectsTable implements BaseColumns {
        public static final String TABLE_NAME = "subjects";
        public static final String COL_NAME = "name";
        public static final String COL_COLOR_HEX = "color_hex";
        public static final String COL_CREATED_AT = "created_at";
    }

    public static final class MaterialsTable implements BaseColumns {
        public static final String TABLE_NAME = "materials";
        public static final String COL_SUBJECT_ID = "subject_id";
        public static final String COL_TITLE = "title";
        public static final String COL_FILE_PATH = "file_path";
        public static final String COL_PAGE_COUNT = "page_count";
        public static final String COL_EXTRACTED_TEXT = "extracted_text";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_LAST_OPENED_AT = "last_opened_at";
    }

    public static final class SummariesTable implements BaseColumns {
        public static final String TABLE_NAME = "summaries";
        public static final String COL_MATERIAL_ID = "material_id";
        public static final String COL_STYLE = "style"; // CONCISE, DETAILED, EXAM_CHEAT_SHEET
        public static final String COL_OVERVIEW = "overview";
        public static final String COL_KEY_POINTS_JSON = "key_points_json";
        public static final String COL_KEY_TERMS_JSON = "key_terms_json";
        public static final String COL_GENERATED_AT = "generated_at";
    }

    public static final class FlashcardsTable implements BaseColumns {
        public static final String TABLE_NAME = "flashcards";
        public static final String COL_MATERIAL_ID = "material_id";
        public static final String COL_TOPIC = "topic";
        public static final String COL_FRONT_TEXT = "front_text";
        public static final String COL_BACK_TEXT = "back_text";
        public static final String COL_IS_MASTERED = "is_mastered"; // 0 or 1
        public static final String COL_CREATED_AT = "created_at";
    }

    // ==================== PERSON 2 TABLES ====================

    public static final class QuizAttemptsTable implements BaseColumns {
        public static final String TABLE_NAME = "quiz_attempts";
        public static final String COL_MATERIAL_ID = "material_id";
        public static final String COL_SCORE = "score";
        public static final String COL_TOTAL_QUESTIONS = "total_questions";
        public static final String COL_PERCENTAGE = "percentage";
        public static final String COL_TAKEN_AT = "taken_at";
    }

    public static final class StudySessionsTable implements BaseColumns {
        public static final String TABLE_NAME = "study_sessions";
        public static final String COL_MATERIAL_ID = "material_id";
        public static final String COL_SUBJECT_ID = "subject_id";
        public static final String COL_DURATION_SECONDS = "duration_seconds";
        public static final String COL_COMPLETED_AT = "completed_at";
    }

    public static final class TopicPerformanceTable implements BaseColumns {
        public static final String TABLE_NAME = "topic_performance";
        public static final String COL_SUBJECT_ID = "subject_id";
        public static final String COL_TOPIC_NAME = "topic_name";
        public static final String COL_CORRECT_COUNT = "correct_count";
        public static final String COL_TOTAL_COUNT = "total_count";
        public static final String COL_MASTERY_SCORE = "mastery_score";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class ReadinessSnapshotsTable implements BaseColumns {
        public static final String TABLE_NAME = "readiness_snapshots";
        public static final String COL_SUBJECT_ID = "subject_id";
        public static final String COL_READINESS_SCORE = "readiness_score";
        public static final String COL_NOTES = "notes";
        public static final String COL_CALCULATED_AT = "calculated_at";
    }
}

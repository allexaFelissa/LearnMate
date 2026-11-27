# LearnMate – AI Study Assistant (Android / Java)

Backend-first implementation of **Person 1 (Material & AI Module)** and the shared core infrastructure for **Person 2 (Quiz & Learning Progress Module)**.

## Quick Start (Android Studio)

1. Open `d:\projects\LearnMate` in **Android Studio**.
2. Add your Gemini API key to `local.properties` (this file is gitignored; see `local.properties.example`):
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key
   ```
3. Click **Sync Project with Gradle Files** and run on an Android emulator or device (API 26+).

## Shared Core for Person 2

Person 2 can build directly on top of these already-implemented shared classes:

- **Database Schema:** `core/db/DbContract.java` & `core/db/LearnMateDbHelper.java`  
  Already creates all 8 tables (`subjects`, `materials`, `summaries`, `flashcards`, `quiz_attempts`, `study_sessions`, `topic_performance`, `readiness_snapshots`) with foreign keys and cascade deletes enabled.
  - Add Person 2 DAOs in `core/db/dao/`: `QuizAttemptDao`, `StudySessionDao`, `TopicPerformanceDao`, `ReadinessDao`.
  - Read material text for quiz generation via `new MaterialDao(context).getMaterialById(materialId).getExtractedText()`.
- **Gemini API Client:** `core/ai/GeminiClient.java`  
  Call `GeminiClient.getInstance().generateContent(prompt, true, callback)` to request structured JSON quizzes.
- **Background Threads:** `core/utils/AppExecutors.java` (`diskIO()`, `networkIO()`, `mainThread()`).
- **Navigation Extras:** `core/navigation/NavExtras.java` (`EXTRA_MATERIAL_ID`, `EXTRA_MATERIAL_TITLE`, `EXTRA_SUBJECT_ID`).
- **UI Hooks to Wire Up:**
  - `DashboardActivity.java`: `btnProgressPlaceholder`, `btnReadinessPlaceholder`, `btnTimerPlaceholder`, plus `tvReadinessPlaceholder`.
  - `MaterialDetailActivity.java`: `btnTakeQuiz`, `btnStudyTimer`.

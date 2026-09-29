-- V5: fine-grained per-lesson progress (resume position, personal notes)
-- Matches entity: com.gobro_backend.progress.LessonProgress

CREATE TABLE lesson_progress (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id        BIGINT      NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    watched_seconds  INTEGER     NOT NULL DEFAULT 0,
    playback_speed   DOUBLE PRECISION,
    completed        BOOLEAN     NOT NULL DEFAULT FALSE,
    personal_note    TEXT,
    last_watched_at  TIMESTAMP,
    completed_at     TIMESTAMP,

    CONSTRAINT uk_progress_student_lesson UNIQUE (student_id, lesson_id)
);

CREATE INDEX idx_progress_student_lesson ON lesson_progress (student_id, lesson_id);
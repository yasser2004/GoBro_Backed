-- V3: lessons (individual videos) within a course section
-- Matches entity: com.gobro_backend.lesson.Lesson

CREATE TABLE lessons (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(150)  NOT NULL,
    description      VARCHAR(1000),
    position         INTEGER       NOT NULL,
    video_url        VARCHAR(500)  NOT NULL,
    duration_seconds INTEGER,
    subtitles_url    VARCHAR(500),
    resource_url     VARCHAR(500),
    free_preview     BOOLEAN       NOT NULL DEFAULT FALSE,
    section_id       BIGINT        NOT NULL REFERENCES course_sections(id) ON DELETE CASCADE,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lessons_section_position ON lessons (section_id, position);
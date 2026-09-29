-- V2: courses and their playlist sections (chapters)
-- Matches entities: com.gobro_backend.course.entity.{Course, CourseSection, CourseStatus}

CREATE TABLE courses (
    id                        BIGSERIAL PRIMARY KEY,
    title                     VARCHAR(150)   NOT NULL,
    slug                      VARCHAR(180)   NOT NULL,
    description               TEXT           NOT NULL,
    objectives                TEXT,
    prerequisites             TEXT,
    thumbnail_url             VARCHAR(500),
    category                  VARCHAR(80)    NOT NULL,
    language                  VARCHAR(10)    NOT NULL DEFAULT 'fr',
    price                     NUMERIC(10,2)  NOT NULL DEFAULT 0,
    free                      BOOLEAN        NOT NULL DEFAULT FALSE,
    level_test_threshold      DOUBLE PRECISION,
    level_test_question_count INTEGER,
    status                    VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    rejection_reason          VARCHAR(500),
    instructor_id             BIGINT         NOT NULL REFERENCES users(id),
    total_duration_minutes    INTEGER        NOT NULL DEFAULT 0,
    students_count            INTEGER        NOT NULL DEFAULT 0,
    average_rating            DOUBLE PRECISION NOT NULL DEFAULT 0,
    reviews_count             INTEGER        NOT NULL DEFAULT 0,
    published_at              TIMESTAMP,
    created_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_courses_slug ON courses (slug);
CREATE INDEX idx_courses_status ON courses (status);
CREATE INDEX idx_courses_instructor ON courses (instructor_id);

CREATE TABLE course_sections (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(150)  NOT NULL,
    description      VARCHAR(1000),
    position         INTEGER       NOT NULL,
    video_url        VARCHAR(500),
    duration_seconds INTEGER,
    free_preview     BOOLEAN       NOT NULL DEFAULT FALSE,
    course_id        BIGINT        NOT NULL REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX idx_sections_course_position ON course_sections (course_id, position);
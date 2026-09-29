-- V4: enrollments (student <-> course access) and completed-lessons tracking
-- Matches entity: com.gobro_backend.enrollment.Enrollment (+ UnlockMethod enum)

CREATE TABLE enrollments (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id        BIGINT           NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    unlock_method    VARCHAR(20)      NOT NULL, -- PAYMENT | LEVEL_TEST | FREE | COUPON
    progress_percent DOUBLE PRECISION NOT NULL DEFAULT 0,
    last_lesson_id   BIGINT REFERENCES lessons(id),
    last_accessed_at TIMESTAMP,
    completed_at     TIMESTAMP,
    enrolled_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_enrollment_student_course UNIQUE (student_id, course_id)
);

CREATE INDEX idx_enrollments_student ON enrollments (student_id);
CREATE INDEX idx_enrollments_course ON enrollments (course_id);

-- @ElementCollection Set<Long> completedLessonIds on Enrollment
CREATE TABLE enrollment_completed_lessons (
    enrollment_id BIGINT NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    lesson_id     BIGINT NOT NULL,

    PRIMARY KEY (enrollment_id, lesson_id)
);
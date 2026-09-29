package com.gobro_backend.progress;


import com.gobro_backend.lesson.Lesson;
import com.gobro_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Fine-grained, per-lesson tracking for a student — cahier des charges §7.4
 * (Vidéos : "Marque-page", "Notes personnelles", vitesse de lecture) and
 * §11 (Tableau de bord Étudiant -> progression).
 *
 * This complements {@link com.gobro_backend.enrollment.Enrollment}, which
 * only stores the aggregate course-level progress (percentage, set of
 * completed lesson ids): LessonProgress is the source of truth for the
 * exact playback position to resume from and any personal note, one row
 * per (student, lesson).
 */
@Entity
@Table(name = "lesson_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_progress_student_lesson", columnNames = {"student_id", "lesson_id"})
}, indexes = {
        @Index(name = "idx_progress_student_lesson", columnList = "student_id, lesson_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    /** Playback position in seconds — resume point / "marque-page". */
    @Column(nullable = false)
    @Builder.Default
    private int watchedSeconds = 0;

    /** Preferred playback speed for this lesson (e.g. 1.0, 1.25, 1.5, 2.0). */
    @Column
    private Double playbackSpeed;

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    /** Free-form personal note the student attaches to this lesson. */
    @Column(columnDefinition = "TEXT")
    private String personalNote;

    private LocalDateTime lastWatchedAt;

    private LocalDateTime completedAt;

    public void markCompleted() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
    }
}
package com.gobro_backend.enrollment;


import com.gobro_backend.course.entity.Course;
import com.gobro_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks a student's access to and progress through a course — cahier des
 * charges §11 (Tableau de bord Étudiant : cours suivis, progression) and
 * §12 (Tableau de bord Formateur : liste des étudiants).
 *
 * One row per (student, course) pair, created either by a successful
 * payment, a level-test success (§9 Déblocage des cours), or a free course.
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_enrollment_student_course", columnNames = {"student_id", "course_id"})
}, indexes = {
        @Index(name = "idx_enrollments_student", columnList = "student_id"),
        @Index(name = "idx_enrollments_course", columnList = "course_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** How the student gained access — see cahier des charges §9 and §10. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnlockMethod unlockMethod;

    @Column(nullable = false)
    @Builder.Default
    private double progressPercent = 0.0;

    /** IDs of lessons the student has watched to completion. */
    @ElementCollection
    @CollectionTable(name = "enrollment_completed_lessons", joinColumns = @JoinColumn(name = "enrollment_id"))
    @Column(name = "lesson_id")
    @Builder.Default
    private Set<Long> completedLessonIds = new HashSet<>();

    /** Last lesson watched — used to resume playback ("Marque-page", §7.4). */
    private Long lastLessonId;

    private LocalDateTime lastAccessedAt;

    private LocalDateTime completedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    public boolean isCompleted() {
        return completedAt != null;
    }

    public enum UnlockMethod {
        /** Paid via D17 / Konnect / Flouci / carte bancaire / wallet — §10. */
        PAYMENT,
        /** Unlocked for free by passing the level test — §9. */
        LEVEL_TEST,
        /** Course marked as free by the instructor. */
        FREE,
        /** Unlocked via a coupon / promo code — §10. */
        COUPON
    }
}
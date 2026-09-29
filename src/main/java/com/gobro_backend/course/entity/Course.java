package com.gobro_backend.course.entity;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.gobro_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A formation published (or being authored) by a Formateur.
 * Covers cahier des charges §7.3 (Cours), §9 (Déblocage des cours via test
 * de niveau) and §13 (validation / modération par l'administrateur).
 */
@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_courses_slug", columnList = "slug", unique = true),
        @Index(name = "idx_courses_status", columnList = "status"),
        @Index(name = "idx_courses_instructor", columnList = "instructor_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    /** URL-friendly unique identifier, generated from the title. */
    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String objectives;

    @Column(columnDefinition = "TEXT")
    private String prerequisites;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String language = "fr";

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean free = false;

    /**
     * Score threshold (percentage, e.g. 90.0) required on the level test to
     * unlock this course for free instead of paying — cahier des charges §9.
     * Null means the course has no level-test unlock path.
     */
    @Column
    private Double levelTestThreshold;

    @Column
    private Integer levelTestQuestionCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    /** Set by an admin when status = REJECTED (see CourseService#rejectCourse). */
    @Column(length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @JsonManagedReference
    @Builder.Default
    private List<CourseSection> sections = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private int totalDurationMinutes = 0;

    @Column(nullable = false)
    @Builder.Default
    private int studentsCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private double averageRating = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private int reviewsCount = 0;

    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addSection(CourseSection section) {
        section.setCourse(this);
        this.sections.add(section);
    }

    public void removeSection(CourseSection section) {
        this.sections.remove(section);
        section.setCourse(null);
    }
}

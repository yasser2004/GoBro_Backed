package com.gobro_backend.lesson;


import com.gobro_backend.course.entity.CourseSection;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A single playable lesson (video) within a {@link CourseSection} chapter —
 * cahier des charges §7.3 (Playlist) and §7.4 (Vidéos : streaming, qualité
 * automatique, vitesse de lecture, sous-titres, marque-page, notes
 * personnelles, téléchargements).
 *
 * CourseSection acts as the chapter/grouping; Lesson is the actual content
 * unit students watch and progress through.
 */
@Entity
@Table(name = "lessons", indexes = {
        @Index(name = "idx_lessons_section_position", columnList = "section_id, position")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    /** Order of this lesson within its section, starting at 0. */
    @Column(nullable = false)
    private int position;

    /** Streaming URL / object storage key (see StorageConfig: S3 / R2 / MinIO). */
    @Column(nullable = false, length = 500)
    private String videoUrl;

    @Column
    private Integer durationSeconds;

    /** VTT/SRT subtitles URL — cahier des charges §7.4 "Sous-titres". */
    @Column(length = 500)
    private String subtitlesUrl;

    /** Downloadable resource (PDF, slides, code samples) — §7.3 "Téléchargements". */
    @Column(length = 500)
    private String resourceUrl;

    /** Free preview: playable without purchasing the course. */
    @Column(nullable = false)
    @Builder.Default
    private boolean freePreview = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private CourseSection section;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
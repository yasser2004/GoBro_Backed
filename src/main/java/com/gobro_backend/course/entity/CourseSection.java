package com.gobro_backend.course.entity;




import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.gobro_backend.lesson.Lesson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A chapter/grouping within a course's playlist (cahier des charges §7.3
 * Cours -> Playlist). The actual watchable content units are {@link Lesson}s
 * nested inside this section, ordered via {@link #position}.
 */
@Entity
@Table(name = "course_sections", indexes = {
        @Index(name = "idx_sections_course_position", columnList = "course_id, position")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    /** Order of this section within the course playlist, starting at 0. */
    @Column(nullable = false)
    private int position;

    @Column(name = "free_preview", nullable = false)
    @Builder.Default
    private boolean freePreview = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonBackReference
    private Course course;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @JsonManagedReference
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();

    public void addLesson(Lesson lesson) {
        lesson.setSection(this);
        this.lessons.add(lesson);
    }

    public void removeLesson(Lesson lesson) {
        this.lessons.remove(lesson);
        lesson.setSection(null);
    }
}
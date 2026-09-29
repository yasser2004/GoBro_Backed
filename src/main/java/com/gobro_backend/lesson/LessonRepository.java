package com.gobro_backend.lesson;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findBySectionIdOrderByPositionAsc(Long sectionId);

    Optional<Lesson> findByIdAndSectionId(Long id, Long sectionId);

    long countBySectionId(Long sectionId);

    void deleteBySectionId(Long sectionId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(l) FROM Lesson l WHERE l.section.course.id = :courseId
            """)
    long countByCourseId(@org.springframework.data.repository.query.Param("courseId") Long courseId);
}

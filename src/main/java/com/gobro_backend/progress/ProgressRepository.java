package com.gobro_backend.progress;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByStudentIdAndLessonId(Long studentId, Long lessonId);

    @Query("""
            SELECT lp FROM LessonProgress lp
            WHERE lp.student.id = :studentId AND lp.lesson.section.course.id = :courseId
            ORDER BY lp.lesson.section.position ASC, lp.lesson.position ASC
            """)
    List<LessonProgress> findByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("""
            SELECT lp FROM LessonProgress lp
            WHERE lp.student.id = :studentId AND lp.lesson.section.course.id = :courseId
            ORDER BY lp.lastWatchedAt DESC
            """)
    List<LessonProgress> findMostRecentByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    long countByStudentIdAndLesson_Section_Course_IdAndCompletedTrue(Long studentId, Long courseId);
}
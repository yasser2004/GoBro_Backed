package com.gobro_backend.course.repository;

import com.gobro_backend.course.entity.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    List<CourseSection> findByCourseIdOrderByPositionAsc(Long courseId);

    Optional<CourseSection> findByIdAndCourseId(Long id, Long courseId);

    void deleteByCourseId(Long courseId);

    long countByCourseId(Long courseId);

}
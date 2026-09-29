package com.gobro_backend.course.repository;


import com.gobro_backend.course.entity.Course;
import com.gobro_backend.course.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findByInstructorIdOrderByCreatedAtDesc(Long instructorId, Pageable pageable);

    Optional<Course> findByIdAndInstructorId(Long id, Long instructorId);

    Page<Course> findByStatusAndCategoryIgnoreCase(CourseStatus status, String category, Pageable pageable);

    @Query("""
            SELECT c FROM Course c
            WHERE c.status = :status
              AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Course> searchPublished(@Param("status") CourseStatus status, @Param("keyword") String keyword, Pageable pageable);

    long countByStatus(CourseStatus status);

    long countByInstructorId(Long instructorId);
}
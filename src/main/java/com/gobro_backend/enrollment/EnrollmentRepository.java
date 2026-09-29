package com.gobro_backend.enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    Page<Enrollment> findByStudentIdOrderByEnrolledAtDesc(Long studentId, Pageable pageable);

    Page<Enrollment> findByCourseIdOrderByEnrolledAtDesc(Long courseId, Pageable pageable);

    long countByCourseId(Long courseId);

    long countByStudentId(Long studentId);

    long countByCourseIdAndCompletedAtIsNotNull(Long courseId);
}
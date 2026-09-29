package com.gobro_backend.enrollment;


import com.gobro_backend.course.entity.Course;
import com.gobro_backend.course.entity.CourseStatus;
import com.gobro_backend.course.repository.CourseRepository;
import com.gobro_backend.lesson.LessonRepository;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Grants and tracks a student's access to a course — cahier des charges
 * §9 (Déblocage des cours), §10 (Paiement), §11 (Tableau de bord Étudiant)
 * and §12 (Tableau de bord Formateur -> liste des étudiants).
 *
 * NOTE: this service only records the enrollment once access has already
 * been granted. The actual payment capture (D17/Konnect/Flouci/carte/wallet)
 * and the level-test scoring happen in their own dedicated modules, which
 * call {@link #enroll} once they've confirmed the student is entitled.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Transactional
    public Enrollment enroll(String studentEmail, Long courseId, Enrollment.UnlockMethod unlockMethod) {
        User student = findUser(studentEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable"));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce cours n'est pas disponible à l'achat");
        }

        return enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseGet(() -> {
                    Enrollment enrollment = Enrollment.builder()
                            .student(student)
                            .course(course)
                            .unlockMethod(unlockMethod)
                            .progressPercent(0.0)
                            .build();
                    Enrollment saved = enrollmentRepository.save(enrollment);

                    course.setStudentsCount(course.getStudentsCount() + 1);
                    courseRepository.save(course);

                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(String studentEmail, Long courseId) {
        User student = findUser(studentEmail);
        return enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId);
    }

    @Transactional(readOnly = true)
    public Enrollment getProgress(String studentEmail, Long courseId) {
        User student = findUser(studentEmail);
        return findEnrollment(student.getId(), courseId);
    }

    @Transactional(readOnly = true)
    public Page<Enrollment> listMyEnrollments(String studentEmail, Pageable pageable) {
        User student = findUser(studentEmail);
        return enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(student.getId(), pageable);
    }

    /**
     * Marks a lesson as watched/completed and recalculates overall progress
     * against the total number of lessons in the course. Completing the
     * last lesson sets completedAt (hook point for certificate issuance
     * and XP gamification rewards — see cahier des charges §8, §7.7).
     */
    @Transactional
    public Enrollment markLessonCompleted(String studentEmail, Long courseId, Long lessonId) {
        User student = findUser(studentEmail);
        Enrollment enrollment = findEnrollment(student.getId(), courseId);

        enrollment.getCompletedLessonIds().add(lessonId);
        enrollment.setLastLessonId(lessonId);
        enrollment.setLastAccessedAt(LocalDateTime.now());

        recalculateProgress(enrollment, courseId);

        return enrollmentRepository.save(enrollment);
    }

    /**
     * Removes a lesson from the completed set and recalculates the course
     * progress. A previously completed course becomes incomplete again when
     * one of its lessons is unchecked.
     */
    @Transactional
    public Enrollment markLessonIncomplete(String studentEmail, Long courseId, Long lessonId) {
        User student = findUser(studentEmail);
        Enrollment enrollment = findEnrollment(student.getId(), courseId);

        enrollment.getCompletedLessonIds().remove(lessonId);
        enrollment.setLastAccessedAt(LocalDateTime.now());
        recalculateProgress(enrollment, courseId);

        return enrollmentRepository.save(enrollment);
    }

    /**
     * Instructor dashboard: students enrolled in one of their courses (§12).
     * Restricted to the owning instructor or an admin.
     */
    @Transactional(readOnly = true)
    public Page<Enrollment> listCourseStudents(String requesterEmail, Long courseId, Pageable pageable) {
        User requester = findUser(requesterEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable"));

        boolean isOwner = course.getInstructor().getId().equals(requester.getId());
        boolean isAdmin = "ADMIN".equals(requester.getRole().getName());
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas propriétaire de ce cours");
        }

        return enrollmentRepository.findByCourseIdOrderByEnrolledAtDesc(courseId, pageable);
    }

    // ---------- Helpers ----------

    private Enrollment findEnrollment(Long studentId, Long courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vous n'êtes pas inscrit à ce cours"));
    }

    private void recalculateProgress(Enrollment enrollment, Long courseId) {
        long totalLessons = lessonRepository.countByCourseId(courseId);
        double progress = totalLessons == 0
                ? 0.0
                : (enrollment.getCompletedLessonIds().size() * 100.0) / totalLessons;
        enrollment.setProgressPercent(Math.min(progress, 100.0));

        if (enrollment.getProgressPercent() >= 100.0) {
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now());
            }
        } else {
            enrollment.setCompletedAt(null);
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }
}

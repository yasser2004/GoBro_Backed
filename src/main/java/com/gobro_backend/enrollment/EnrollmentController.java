package com.gobro_backend.enrollment;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Enrollment endpoints — cahier des charges §11 (Tableau de bord Étudiant :
 * cours suivis, progression) and §12 (Tableau de bord Formateur : liste
 * des étudiants).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Inscriptions", description = "Accès aux cours, suivi de progression et liste des étudiants")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // ---------- Student (self-service) ----------

    @PostMapping("/enrollments")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    @Operation(summary = "S'inscrire à un cours déjà débloqué (paiement, test de niveau ou cours gratuit)")
    public ResponseEntity<EnrollmentResponse> enroll(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody EnrollRequest request
    ) {
        Enrollment enrollment = enrollmentService.enroll(
                principal.getUsername(), request.courseId(), request.unlockMethod());
        return ResponseEntity.status(HttpStatus.CREATED).body(EnrollmentResponse.from(enrollment));
    }

    @GetMapping("/enrollments/me")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    @Operation(summary = "Lister mes cours suivis avec leur progression")
    public ResponseEntity<Page<EnrollmentResponse>> myEnrollments(
            @AuthenticationPrincipal UserDetails principal,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                enrollmentService.listMyEnrollments(principal.getUsername(), pageable)
                        .map(EnrollmentResponse::from)
        );
    }

    @GetMapping("/enrollments/me/{courseId}")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    @Operation(summary = "Détail de ma progression sur un cours")
    public ResponseEntity<EnrollmentResponse> myProgress(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(EnrollmentResponse.from(
                enrollmentService.getProgress(principal.getUsername(), courseId)));
    }

    @PatchMapping("/enrollments/me/{courseId}/lessons/{lessonId}/complete")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    @Operation(summary = "Marquer une leçon comme terminée et recalculer la progression")
    public ResponseEntity<EnrollmentResponse> completeLesson(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId,
            @PathVariable Long lessonId
    ) {
        Enrollment enrollment = enrollmentService.markLessonCompleted(principal.getUsername(), courseId, lessonId);
        return ResponseEntity.ok(EnrollmentResponse.from(enrollment));
    }

    @DeleteMapping("/enrollments/me/{courseId}/lessons/{lessonId}/complete")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    @Operation(summary = "Retirer une leçon des leçons terminées et recalculer la progression")
    public ResponseEntity<EnrollmentResponse> uncompleteLesson(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId,
            @PathVariable Long lessonId
    ) {
        Enrollment enrollment = enrollmentService.markLessonIncomplete(principal.getUsername(), courseId, lessonId);
        return ResponseEntity.ok(EnrollmentResponse.from(enrollment));
    }

    // ---------- Instructor dashboard (§12) ----------

    @GetMapping("/courses/manage/{courseId}/students")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
    @Operation(summary = "Lister les étudiants inscrits à l'un de mes cours")
    public ResponseEntity<Page<EnrollmentResponse>> courseStudents(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                enrollmentService.listCourseStudents(principal.getUsername(), courseId, pageable)
                        .map(EnrollmentResponse::from)
        );
    }

    // --- small request/response records local to this controller ---

    public record EnrollRequest(
            @NotNull Long courseId,
            @NotNull Enrollment.UnlockMethod unlockMethod
    ) {
    }

    public record EnrollmentResponse(
            Long id,
            Long courseId,
            String courseTitle,
            Long studentId,
            String studentFullName,
            String unlockMethod,
            double progressPercent,
            Set<Long> completedLessonIds,
            Long lastLessonId,
            boolean completed,
            LocalDateTime enrolledAt,
            LocalDateTime lastAccessedAt,
            LocalDateTime completedAt
    ) {
        static EnrollmentResponse from(Enrollment e) {
            return new EnrollmentResponse(
                    e.getId(),
                    e.getCourse().getId(),
                    e.getCourse().getTitle(),
                    e.getStudent().getId(),
                    e.getStudent().getFullName(),
                    e.getUnlockMethod().name(),
                    e.getProgressPercent(),
                    e.getCompletedLessonIds(),
                    e.getLastLessonId(),
                    e.isCompleted(),
                    e.getEnrolledAt(),
                    e.getLastAccessedAt(),
                    e.getCompletedAt()
            );
        }
    }
}

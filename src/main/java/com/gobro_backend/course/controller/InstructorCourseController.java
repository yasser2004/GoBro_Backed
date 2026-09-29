package com.gobro_backend.course.controller;




import com.gobro_backend.course.dto.CourseResponse;
import com.gobro_backend.course.dto.CreateCourseRequest;
import com.gobro_backend.course.dto.UpdateCourseRequest;
import com.gobro_backend.course.entity.CourseSection;
import com.gobro_backend.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Course authoring for Formateur (create/update/submit, sections/playlist
 * management) and validation workflow for Admin (approve/reject) — cahier
 * des charges §7.3, §9, §12 (Tableau de bord Formateur) and §13
 * (Administration -> Validation des cours). Matches the
 * "/api/v1/courses/manage/**" pattern (FORMATEUR or ADMIN) in SecurityConfig.
 *
 * Sections here are chapters/groupings only. The actual watchable content
 * (video, subtitles, resources) is added via LessonController, nested
 * under /api/v1/courses/manage/{courseId}/sections/{sectionId}/lessons.
 */
@RestController
@RequestMapping("/api/v1/courses/manage")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
@Tag(name = "Gestion des cours", description = "Création, édition et validation des formations")
public class InstructorCourseController {

    private final CourseService courseService;

    @PostMapping
    @Operation(summary = "Créer un nouveau cours (statut initial : DRAFT)")
    public ResponseEntity<CourseResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateCourseRequest request
    ) {
        CourseResponse created = courseService.createCourse(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Lister mes cours (formateur) — l'admin voit uniquement les siens ici aussi")
    public ResponseEntity<Page<CourseResponse>> listMine(
            @AuthenticationPrincipal UserDetails principal,
            Pageable pageable
    ) {
        return ResponseEntity.ok(courseService.listInstructorCourses(principal.getUsername(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un de mes cours, quel que soit son statut")
    public ResponseEntity<CourseResponse> getOne(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(courseService.getForInstructor(principal.getUsername(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un cours (repasse en PENDING_VALIDATION s'il était déjà publié)")
    public ResponseEntity<CourseResponse> update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest request
    ) {
        return ResponseEntity.ok(courseService.updateCourse(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un cours (impossible si déjà publié)")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ) {
        courseService.deleteCourse(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Soumettre le cours à la validation de l'administration")
    public ResponseEntity<CourseResponse> submit(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(courseService.submitForValidation(principal.getUsername(), id));
    }

    // ---------- Sections (chapters) ----------
    // Lesson (video) management lives in LessonController, nested under
    // /{id}/sections/{sectionId}/lessons.

    @PostMapping("/{id}/sections")
    @Operation(summary = "Ajouter une section (chapitre) à la playlist du cours")
    public ResponseEntity<CourseResponse> addSection(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody AddSectionRequest request
    ) {
        CourseSection section = CourseSection.builder()
                .title(request.title())
                .description(request.description())
                .build();

        return ResponseEntity.ok(courseService.addSection(principal.getUsername(), id, section));
    }

    @PutMapping("/{id}/sections/{sectionId}")
    @Operation(summary = "Modifier le nom d'une section")
    public ResponseEntity<CourseResponse> updateSection(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @PathVariable Long sectionId,
            @Valid @RequestBody AddSectionRequest request
    ) {
        return ResponseEntity.ok(courseService.updateSection(
                principal.getUsername(), id, sectionId, request.title(), request.description()));
    }

    @DeleteMapping("/{id}/sections/{sectionId}")
    @Operation(summary = "Supprimer une section de la playlist")
    public ResponseEntity<Void> deleteSection(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @PathVariable Long sectionId
    ) {
        courseService.deleteSection(principal.getUsername(), id, sectionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/sections/reorder")
    @Operation(summary = "Réordonner les sections de la playlist")
    public ResponseEntity<CourseResponse> reorderSections(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody ReorderSectionsRequest request
    ) {
        return ResponseEntity.ok(courseService.reorderSections(principal.getUsername(), id, request.orderedSectionIds()));
    }

    // ---------- Admin validation (§13) ----------

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approuver et publier un cours (admin)")
    public ResponseEntity<CourseResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.approveCourse(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeter un cours avec motif (admin)")
    public ResponseEntity<CourseResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectCourseRequest request
    ) {
        return ResponseEntity.ok(courseService.rejectCourse(id, request.reason()));
    }

    // --- small request records local to this controller ---

    public record AddSectionRequest(
            @NotBlank String title,
            String description
    ) {
    }

    public record ReorderSectionsRequest(@NotEmpty List<Long> orderedSectionIds) {
    }

    public record RejectCourseRequest(@NotBlank String reason) {
    }
}
package com.gobro_backend.lesson;


import com.gobro_backend.course.dto.CourseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lesson (video) authoring within a course section — cahier des charges
 * §7.3 (Playlist) and §7.4 (Vidéos). Reserved to the owning Formateur or
 * an Admin, matching InstructorCourseController's ownership rules.
 */
@RestController
@RequestMapping("/api/v1/courses/manage/{courseId}/sections/{sectionId}/lessons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
@Tag(name = "Gestion des leçons", description = "Création et organisation des leçons (vidéos) d'une section")
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    @Operation(summary = "Ajouter une leçon (vidéo) à une section")
    public ResponseEntity<CourseResponse> addLesson(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody AddLessonRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                lessonService.addLesson(principal.getUsername(), courseId, sectionId, request));
    }

    @PutMapping("/{lessonId}")
    @Operation(summary = "Modifier une leçon")
    public ResponseEntity<CourseResponse> updateLesson(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @Valid @RequestBody AddLessonRequest request
    ) {
        return ResponseEntity.ok(
                lessonService.updateLesson(principal.getUsername(), courseId, sectionId, lessonId, request));
    }

    @DeleteMapping("/{lessonId}")
    @Operation(summary = "Supprimer une leçon")
    public ResponseEntity<Void> deleteLesson(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @PathVariable Long lessonId
    ) {
        lessonService.deleteLesson(principal.getUsername(), courseId, sectionId, lessonId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Réordonner les leçons d'une section")
    public ResponseEntity<CourseResponse> reorderLessons(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody ReorderLessonsRequest request
    ) {
        return ResponseEntity.ok(
                lessonService.reorderLessons(principal.getUsername(), courseId, sectionId, request.orderedLessonIds()));
    }

    public record AddLessonRequest(
            @NotBlank String title,
            String description,
            @NotBlank String videoUrl,
            Integer durationSeconds,
            String subtitlesUrl,
            String resourceUrl,
            Boolean freePreview
    ) {
    }

    public record ReorderLessonsRequest(@NotEmpty List<Long> orderedLessonIds) {
    }
}
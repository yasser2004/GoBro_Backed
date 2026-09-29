package com.gobro_backend.progress;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Per-lesson progress endpoints for the video player: resume position
 * ("marque-page"), completion, personal notes and playlist checkmarks —
 * cahier des charges §7.4 and §11. All endpoints operate on the
 * authenticated student's own progress.
 */
@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
@Tag(name = "Progression", description = "Reprise de lecture, notes personnelles et avancement par leçon")
public class ProgressController {

    private final ProgressService progressService;

    @PutMapping("/lessons/{lessonId}")
    @Operation(summary = "Mettre à jour la position de lecture (heartbeat du lecteur vidéo)")
    public ResponseEntity<ProgressResponse> updatePosition(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdatePositionRequest request
    ) {
        LessonProgress progress = progressService.updatePlaybackPosition(
                principal.getUsername(), lessonId, request.watchedSeconds(), request.playbackSpeed());
        return ResponseEntity.ok(ProgressResponse.from(progress));
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @Operation(summary = "Marquer explicitement une leçon comme terminée")
    public ResponseEntity<ProgressResponse> complete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long lessonId
    ) {
        return ResponseEntity.ok(ProgressResponse.from(
                progressService.markCompleted(principal.getUsername(), lessonId)));
    }

    @PutMapping("/lessons/{lessonId}/note")
    @Operation(summary = "Enregistrer une note personnelle sur une leçon")
    public ResponseEntity<ProgressResponse> saveNote(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long lessonId,
            @Valid @RequestBody SaveNoteRequest request
    ) {
        return ResponseEntity.ok(ProgressResponse.from(
                progressService.saveNote(principal.getUsername(), lessonId, request.note())));
    }

    @GetMapping("/lessons/{lessonId}")
    @Operation(summary = "Récupérer ma progression sur une leçon")
    public ResponseEntity<ProgressResponse> getLessonProgress(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long lessonId
    ) {
        return ResponseEntity.ok(ProgressResponse.from(
                progressService.getLessonProgress(principal.getUsername(), lessonId)));
    }

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "Lister ma progression sur toutes les leçons d'un cours (coches de la playlist)")
    public ResponseEntity<List<ProgressResponse>> listCourseProgress(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId
    ) {
        List<ProgressResponse> result = progressService.listCourseProgress(principal.getUsername(), courseId)
                .stream()
                .map(ProgressResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/courses/{courseId}/resume")
    @Operation(summary = "Récupérer le point de reprise (dernière leçon regardée) d'un cours")
    public ResponseEntity<ProgressResponse> resume(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long courseId
    ) {
        Optional<LessonProgress> resumePoint = progressService.getResumePoint(principal.getUsername(), courseId);
        return resumePoint
                .map(progress -> ResponseEntity.ok(ProgressResponse.from(progress)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // --- small request/response records local to this controller ---

    public record UpdatePositionRequest(
            @NotNull @Min(0) Integer watchedSeconds,
            Double playbackSpeed
    ) {
    }

    public record SaveNoteRequest(String note) {
    }

    public record ProgressResponse(
            Long id,
            Long lessonId,
            String lessonTitle,
            int watchedSeconds,
            Double playbackSpeed,
            boolean completed,
            String personalNote,
            LocalDateTime lastWatchedAt,
            LocalDateTime completedAt
    ) {
        static ProgressResponse from(LessonProgress p) {
            return new ProgressResponse(
                    p.getId(),
                    p.getLesson().getId(),
                    p.getLesson().getTitle(),
                    p.getWatchedSeconds(),
                    p.getPlaybackSpeed(),
                    p.isCompleted(),
                    p.getPersonalNote(),
                    p.getLastWatchedAt(),
                    p.getCompletedAt()
            );
        }
    }
}
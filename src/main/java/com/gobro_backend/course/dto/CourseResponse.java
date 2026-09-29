package com.gobro_backend.course.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Outward-facing projection of a {@link com.gobro_backend.course.entity.Course},
 * used both for the public catalog (published courses) and the instructor's
 * own course management views.
 */
public record CourseResponse(
        Long id,
        String title,
        String slug,
        String description,
        String objectives,
        String prerequisites,
        String thumbnailUrl,
        String category,
        String language,
        BigDecimal price,
        boolean free,
        Double levelTestThreshold,
        Integer levelTestQuestionCount,
        String status,
        String rejectionReason,
        InstructorSummary instructor,
        List<SectionResponse> sections,
        int totalDurationMinutes,
        int studentsCount,
        double averageRating,
        int reviewsCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record InstructorSummary(
            Long id,
            String fullName,
            String avatarUrl
    ) {
    }

    public record SectionResponse(
            Long id,
            String title,
            String description,
            int position,
            List<LessonResponse> lessons
    ) {
    }

    public record LessonResponse(
            Long id,
            String title,
            String description,
            int position,
            String videoUrl,
            Integer durationSeconds,
            String subtitlesUrl,
            String resourceUrl,
            boolean freePreview
    ) {
    }
}
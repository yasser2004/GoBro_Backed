package com.gobro_backend.course.dto;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for PUT /api/v1/courses/manage/{id}. All fields are optional
 * (null = unchanged), allowing partial updates. Only the owning instructor
 * (or an admin) may call this — enforced in CourseService#updateCourse.
 * Editing a PUBLISHED course resets its status to PENDING_VALIDATION
 * so the change can be re-reviewed by an admin.
 */
public record UpdateCourseRequest(

        @Size(min = 5, max = 150, message = "Le titre doit contenir entre 5 et 150 caractères")
        String title,

        @Size(min = 20, message = "La description doit contenir au moins 20 caractères")
        String description,

        String objectives,

        String prerequisites,

        String thumbnailUrl,

        String category,

        String language,

        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
        BigDecimal price,

        Boolean free,

        @DecimalMin(value = "0.0", message = "Le seuil doit être entre 0 et 100")
        @DecimalMax(value = "100.0", message = "Le seuil doit être entre 0 et 100")
        Double levelTestThreshold,

        @Min(value = 1, message = "Le test doit contenir au moins 1 question")
        Integer levelTestQuestionCount
) {
}
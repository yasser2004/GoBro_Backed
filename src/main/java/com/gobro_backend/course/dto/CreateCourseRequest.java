package com.gobro_backend.course.dto;


import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Payload for POST /api/v1/courses/manage (instructor creates a new course,
 * always starts in DRAFT status — see CourseService#createCourse).
 */
public record CreateCourseRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 5, max = 150, message = "Le titre doit contenir entre 5 et 150 caractères")
        String title,

        @NotBlank(message = "La description est obligatoire")
        @Size(min = 20, message = "La description doit contenir au moins 20 caractères")
        String description,

        String objectives,

        String prerequisites,

        String thumbnailUrl,

        @NotBlank(message = "La catégorie est obligatoire")
        String category,

        @Pattern(regexp = "fr|en|ar|tn", message = "Langue non supportée")
        String language,

        @NotNull(message = "Le prix est obligatoire (0 pour un cours gratuit)")
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
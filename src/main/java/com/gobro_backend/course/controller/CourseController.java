package com.gobro_backend.course.controller;

import com.gobro_backend.course.dto.CourseResponse;
import com.gobro_backend.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public course catalog (cahier des charges §7.2 Catalogue): browsing,
 * search and course detail for visitors and students. Matches the
 * "/api/v1/courses/public/**" pattern declared as PUBLIC in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/courses/public")
@RequiredArgsConstructor
@Tag(name = "Catalogue de cours", description = "Recherche, filtres et consultation des formations publiées")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "Lister / rechercher les cours publiés (filtres: mot-clé, catégorie)")
    public ResponseEntity<Page<CourseResponse>> browse(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        return ResponseEntity.ok(courseService.browsePublished(keyword, category, pageable));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Détail d'un cours publié par son slug")
    public ResponseEntity<CourseResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(courseService.getPublishedBySlug(slug));
    }
}
package com.gobro_backend.course.service;


import com.gobro_backend.course.dto.CourseResponse;
import com.gobro_backend.course.dto.CreateCourseRequest;
import com.gobro_backend.course.dto.UpdateCourseRequest;
import com.gobro_backend.course.entity.Course;
import com.gobro_backend.course.entity.CourseSection;
import com.gobro_backend.course.entity.CourseStatus;
import com.gobro_backend.course.mapper.CourseMapper;
import com.gobro_backend.course.repository.CourseRepository;
import com.gobro_backend.course.repository.CourseSectionRepository;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;


import com.gobro_backend.course.dto.CourseResponse;
import com.gobro_backend.course.dto.CreateCourseRequest;
import com.gobro_backend.course.dto.UpdateCourseRequest;
import com.gobro_backend.course.entity.Course;
import com.gobro_backend.course.entity.CourseSection;
import com.gobro_backend.course.entity.CourseStatus;
import com.gobro_backend.course.mapper.CourseMapper;
import com.gobro_backend.course.repository.CourseRepository;
import com.gobro_backend.course.repository.CourseSectionRepository;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Course authoring (instructor), validation workflow (admin) and public
 * catalog browsing. Covers cahier des charges §7.3 (Cours), §9 (Déblocage
 * via test de niveau) and §13 (Administration -> Validation des cours).
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    // ---------- Public catalog ----------

    @Transactional(readOnly = true)
    public Page<CourseResponse> browsePublished(String keyword, String category, Pageable pageable) {
        Page<Course> page;
        if (StringUtils.hasText(keyword)) {
            page = courseRepository.searchPublished(CourseStatus.PUBLISHED, keyword, pageable);
        } else if (StringUtils.hasText(category)) {
            page = courseRepository.findByStatusAndCategoryIgnoreCase(CourseStatus.PUBLISHED, category, pageable);
        } else {
            page = courseRepository.findByStatus(CourseStatus.PUBLISHED, pageable);
        }
        return page.map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getPublishedBySlug(String slug) {
        Course course = courseRepository.findBySlug(slug)
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable"));
        return courseMapper.toResponse(course);
    }

    // ---------- Instructor authoring ----------

    @Transactional
    public CourseResponse createCourse(String instructorEmail, @Valid CreateCourseRequest request) {
        User instructor = findUser(instructorEmail);

        Course course = courseMapper.toEntity(request);
        course.setSlug(generateUniqueSlug(request.title()));
        course.setStatus(CourseStatus.DRAFT);
        course.setInstructor(instructor);

        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateCourse(String instructorEmail, Long courseId, @Valid UpdateCourseRequest request) {
        Course course = findOwnedCourseOrAdmin(instructorEmail, courseId);

        courseMapper.updateEntityFromRequest(request, course);

        if (StringUtils.hasText(request.title())) {
            course.setSlug(generateUniqueSlug(request.title(), course.getId()));
        }

        // Any edit to an already-published course must be re-reviewed.
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            course.setStatus(CourseStatus.PENDING_VALIDATION);
        }

        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> listInstructorCourses(String instructorEmail, Pageable pageable) {
        User instructor = findUser(instructorEmail);
        return courseRepository.findByInstructorIdOrderByCreatedAtDesc(instructor.getId(), pageable)
                .map(courseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getForInstructor(String instructorEmail, Long courseId) {
        Course course = findOwnedCourseOrAdmin(instructorEmail, courseId);
        return courseMapper.toResponse(course);
    }

    @Transactional
    public void deleteCourse(String instructorEmail, Long courseId) {
        Course course = findOwnedCourseOrAdmin(instructorEmail, courseId);

        if (course.getStatus() == CourseStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un cours publié ne peut pas être supprimé, archivez-le plutôt");
        }
        courseRepository.delete(course);
    }

    @Transactional
    public CourseResponse submitForValidation(String instructorEmail, Long courseId) {
        Course course = findOwnedCourseOrAdmin(instructorEmail, courseId);

        boolean hasAnyLesson = course.getSections().stream().anyMatch(s -> !s.getLessons().isEmpty());
        if (!hasAnyLesson) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le cours doit contenir au moins une leçon avant soumission");
        }
        if (course.getStatus() != CourseStatus.DRAFT && course.getStatus() != CourseStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seul un cours en brouillon ou rejeté peut être soumis à validation");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(LocalDateTime.now());
        course.setRejectionReason(null);
        return courseMapper.toResponse(courseRepository.save(course));
    }

    // ---------- Admin validation workflow (§13) ----------

    @Transactional
    public CourseResponse approveCourse(Long courseId) {
        Course course = findCourse(courseId);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setRejectionReason(null);
        course.setPublishedAt(LocalDateTime.now());
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse rejectCourse(Long courseId, String reason) {
        Course course = findCourse(courseId);
        course.setStatus(CourseStatus.REJECTED);
        course.setRejectionReason(reason);
        return courseMapper.toResponse(courseRepository.save(course));
    }

    // ---------- Playlist / sections management ----------

    @Transactional
    public CourseResponse addSection(String instructorEmail, Long courseId, CourseSection newSection) {
        Course course = findOwnedCourseOrAdmin(instructorEmail, courseId);

        int nextPosition = course.getSections().stream()
                .mapToInt(CourseSection::getPosition)
                .max()
                .orElse(-1) + 1;
        newSection.setPosition(nextPosition);
        newSection.setCourse(course);
        courseSectionRepository.save(newSection);

        return courseMapper.toResponse(courseRepository.findById(course.getId()).orElseThrow());
    }

    @Transactional
    public CourseResponse updateSection(String instructorEmail, Long courseId, Long sectionId,
                                        String title, String description) {
        Course course = findOwnedCourseOrAdmin(instructorEmail, courseId);
        CourseSection section = courseSectionRepository.findByIdAndCourseId(sectionId, course.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section introuvable"));

        section.setTitle(title);
        section.setDescription(description);
        courseSectionRepository.save(section);
        return courseMapper.toResponse(courseRepository.findById(course.getId()).orElseThrow());
    }

    @Transactional
    public void deleteSection(String instructorEmail, Long courseId, Long sectionId) {
        Course course = findOwnedCourseOrAdmin(instructorEmail, courseId);

        CourseSection section = courseSectionRepository.findByIdAndCourseId(sectionId, course.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section introuvable"));

        int freedSeconds = section.getLessons().stream()
                .filter(l -> l.getDurationSeconds() != null)
                .mapToInt(l -> l.getDurationSeconds())
                .sum();
        course.setTotalDurationMinutes(Math.max(0, course.getTotalDurationMinutes() - freedSeconds / 60));

        course.removeSection(section);
        reindexSectionPositions(course);
        courseRepository.save(course);
    }

    @Transactional
    public CourseResponse reorderSections(String instructorEmail, Long courseId, List<Long> orderedSectionIds) {
        Course course = findOwnedCourseOrAdmin(instructorEmail, courseId);

        List<Long> existingIds = course.getSections().stream().map(CourseSection::getId).toList();
        if (orderedSectionIds.size() != existingIds.size()
                || !new HashSet<>(orderedSectionIds).containsAll(existingIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La liste doit contenir exactement toutes les sections du cours, sans doublon");
        }

        for (int i = 0; i < orderedSectionIds.size(); i++) {
            Long sectionId = orderedSectionIds.get(i);
            CourseSection section = courseSectionRepository.findByIdAndCourseId(sectionId, course.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Section introuvable : " + sectionId));
            section.setPosition(i);
            courseSectionRepository.save(section);
        }

        return courseMapper.toResponse(courseRepository.findById(course.getId()).orElseThrow());
    }

    // ---------- Helpers ----------

    private void reindexSectionPositions(Course course) {
        List<CourseSection> ordered = course.getSections().stream()
                .sorted(Comparator.comparingInt(CourseSection::getPosition))
                .toList();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setPosition(i);
        }
    }

    private Course findOwnedCourseOrAdmin(String requesterEmail, Long courseId) {
        User requester = findUser(requesterEmail);
        Course course = findCourse(courseId);

        boolean isOwner = course.getInstructor().getId().equals(requester.getId());
        boolean isAdmin = "ADMIN".equals(requester.getRole().getName());

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas propriétaire de ce cours");
        }
        return course;
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cours introuvable"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    private String generateUniqueSlug(String title) {
        return generateUniqueSlug(title, null);
    }

    private String generateUniqueSlug(String title, Long excludingCourseId) {
        String base = slugify(title);
        String candidate = base;
        int suffix = 1;

        while (courseRepository.findBySlug(candidate)
                .filter(c -> excludingCourseId == null || !c.getId().equals(excludingCourseId))
                .isPresent()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }
}
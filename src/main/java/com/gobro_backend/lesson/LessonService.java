package com.gobro_backend.lesson;


import com.gobro_backend.course.dto.CourseResponse;
import com.gobro_backend.course.entity.Course;
import com.gobro_backend.course.entity.CourseSection;
import com.gobro_backend.course.mapper.CourseMapper;
import com.gobro_backend.course.repository.CourseRepository;
import com.gobro_backend.course.repository.CourseSectionRepository;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * Lesson authoring within a course section — cahier des charges §7.3 and
 * §7.4. Ownership is enforced the same way as CourseService: only the
 * course's owning instructor or an admin may modify its lessons.
 */
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;

    @Transactional
    public CourseResponse addLesson(String instructorEmail, Long courseId, Long sectionId,
                                    LessonController.AddLessonRequest request) {
        CourseSection section = findOwnedSection(instructorEmail, courseId, sectionId);

        int nextPosition = (int) lessonRepository.countBySectionId(section.getId());

        Lesson lesson = Lesson.builder()
                .title(request.title())
                .description(request.description())
                .videoUrl(request.videoUrl())
                .durationSeconds(request.durationSeconds())
                .subtitlesUrl(request.subtitlesUrl())
                .resourceUrl(request.resourceUrl())
                .freePreview(Boolean.TRUE.equals(request.freePreview()))
                .position(nextPosition)
                .build();

        section.addLesson(lesson);

        Course course = section.getCourse();
        if (request.durationSeconds() != null) {
            course.setTotalDurationMinutes(course.getTotalDurationMinutes() + request.durationSeconds() / 60);
        }
        courseRepository.save(course);

        return courseMapper.toResponse(courseRepository.findById(course.getId()).orElseThrow());
    }

    @Transactional
    public CourseResponse updateLesson(String instructorEmail, Long courseId, Long sectionId, Long lessonId,
                                       LessonController.AddLessonRequest request) {
        CourseSection section = findOwnedSection(instructorEmail, courseId, sectionId);
        Lesson lesson = lessonRepository.findByIdAndSectionId(lessonId, section.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leçon introuvable"));

        Course course = section.getCourse();
        int oldDuration = lesson.getDurationSeconds() == null ? 0 : lesson.getDurationSeconds();
        int newDuration = request.durationSeconds() == null ? 0 : request.durationSeconds();

        lesson.setTitle(request.title());
        lesson.setDescription(request.description());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setDurationSeconds(request.durationSeconds());
        lesson.setSubtitlesUrl(request.subtitlesUrl());
        lesson.setResourceUrl(request.resourceUrl());
        lesson.setFreePreview(Boolean.TRUE.equals(request.freePreview()));
        lessonRepository.save(lesson);

        course.setTotalDurationMinutes(Math.max(0,
                course.getTotalDurationMinutes() + (newDuration - oldDuration) / 60));
        courseRepository.save(course);

        return courseMapper.toResponse(courseRepository.findById(course.getId()).orElseThrow());
    }

    @Transactional
    public void deleteLesson(String instructorEmail, Long courseId, Long sectionId, Long lessonId) {
        CourseSection section = findOwnedSection(instructorEmail, courseId, sectionId);

        Lesson lesson = lessonRepository.findByIdAndSectionId(lessonId, section.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leçon introuvable"));

        Course course = section.getCourse();
        if (lesson.getDurationSeconds() != null) {
            course.setTotalDurationMinutes(
                    Math.max(0, course.getTotalDurationMinutes() - lesson.getDurationSeconds() / 60));
        }

        section.removeLesson(lesson);
        reindexPositions(section);
        courseRepository.save(course);
    }

    @Transactional
    public CourseResponse reorderLessons(String instructorEmail, Long courseId, Long sectionId,
                                         List<Long> orderedLessonIds) {
        CourseSection section = findOwnedSection(instructorEmail, courseId, sectionId);

        List<Long> existingIds = section.getLessons().stream().map(Lesson::getId).toList();
        if (orderedLessonIds.size() != existingIds.size()
                || !new java.util.HashSet<>(orderedLessonIds).containsAll(existingIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La liste doit contenir exactement toutes les leçons de la section, sans doublon");
        }

        for (int i = 0; i < orderedLessonIds.size(); i++) {
            Long lessonId = orderedLessonIds.get(i);
            Lesson lesson = lessonRepository.findByIdAndSectionId(lessonId, section.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Leçon introuvable : " + lessonId));
            lesson.setPosition(i);
            lessonRepository.save(lesson);
        }

        return courseMapper.toResponse(courseRepository.findById(section.getCourse().getId()).orElseThrow());
    }

    // ---------- Helpers ----------

    private void reindexPositions(CourseSection section) {
        List<Lesson> ordered = section.getLessons().stream()
                .sorted(Comparator.comparingInt(Lesson::getPosition))
                .toList();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setPosition(i);
        }
    }

    private CourseSection findOwnedSection(String requesterEmail, Long courseId, Long sectionId) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        CourseSection section = courseSectionRepository.findByIdAndCourseId(sectionId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section introuvable"));

        boolean isOwner = section.getCourse().getInstructor().getId().equals(requester.getId());
        boolean isAdmin = "ADMIN".equals(requester.getRole().getName());
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas propriétaire de ce cours");
        }
        return section;
    }
}
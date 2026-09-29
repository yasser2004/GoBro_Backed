package com.gobro_backend.progress;


import com.gobro_backend.enrollment.EnrollmentService;
import com.gobro_backend.lesson.Lesson;
import com.gobro_backend.lesson.LessonRepository;
import com.gobro_backend.user.entity.User;
import com.gobro_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Per-lesson progress tracking: resume position ("marque-page"), personal
 * notes, playback speed, and auto/explicit completion — cahier des charges
 * §7.4 and §11.
 *
 * When a lesson newly transitions to completed, this service also notifies
 * {@link EnrollmentService} so the course-level aggregate progress
 * (Enrollment.progressPercent / completedLessonIds) stays in sync —
 * LessonProgress is the fine-grained per-lesson source of truth, Enrollment
 * remains the course-level summary used by dashboards and certificates.
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;

    /** Fraction of a lesson's duration watched before it auto-completes (0.9 = 90%). */
    @Value("${app.progress.auto-complete-threshold:0.9}")
    private double autoCompleteThreshold;

    @Transactional
    public LessonProgress updatePlaybackPosition(String studentEmail, Long lessonId, int watchedSeconds, Double playbackSpeed) {
        User student = findUser(studentEmail);
        Lesson lesson = findLesson(lessonId);
        LessonProgress progress = findOrCreate(student, lesson);

        progress.setWatchedSeconds(Math.max(progress.getWatchedSeconds(), watchedSeconds));
        if (playbackSpeed != null) {
            progress.setPlaybackSpeed(playbackSpeed);
        }
        progress.setLastWatchedAt(LocalDateTime.now());

        maybeAutoComplete(progress, lesson, student);

        return progressRepository.save(progress);
    }

    @Transactional
    public LessonProgress markCompleted(String studentEmail, Long lessonId) {
        User student = findUser(studentEmail);
        Lesson lesson = findLesson(lessonId);
        LessonProgress progress = findOrCreate(student, lesson);

        if (!progress.isCompleted()) {
            progress.markCompleted();
            syncEnrollment(student, lesson);
        }
        progress.setLastWatchedAt(LocalDateTime.now());

        return progressRepository.save(progress);
    }

    @Transactional
    public LessonProgress saveNote(String studentEmail, Long lessonId, String note) {
        User student = findUser(studentEmail);
        Lesson lesson = findLesson(lessonId);
        LessonProgress progress = findOrCreate(student, lesson);

        progress.setPersonalNote(note);
        return progressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public LessonProgress getLessonProgress(String studentEmail, Long lessonId) {
        User student = findUser(studentEmail);
        return progressRepository.findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune progression enregistrée pour cette leçon"));
    }

    @Transactional(readOnly = true)
    public List<LessonProgress> listCourseProgress(String studentEmail, Long courseId) {
        User student = findUser(studentEmail);
        return progressRepository.findByStudentIdAndCourseId(student.getId(), courseId);
    }

    /**
     * Returns the most recently watched (incomplete or complete) lesson in
     * the course, used by the player to offer "Reprendre où vous étiez".
     */
    @Transactional(readOnly = true)
    public Optional<LessonProgress> getResumePoint(String studentEmail, Long courseId) {
        User student = findUser(studentEmail);
        return progressRepository.findMostRecentByStudentIdAndCourseId(student.getId(), courseId)
                .stream()
                .findFirst();
    }

    // ---------- Helpers ----------

    private void maybeAutoComplete(LessonProgress progress, Lesson lesson, User student) {
        if (progress.isCompleted() || lesson.getDurationSeconds() == null || lesson.getDurationSeconds() == 0) {
            return;
        }
        double watchedRatio = progress.getWatchedSeconds() / (double) lesson.getDurationSeconds();
        if (watchedRatio >= autoCompleteThreshold) {
            progress.markCompleted();
            syncEnrollment(student, lesson);
        }
    }

    private void syncEnrollment(User student, Lesson lesson) {
        Long courseId = lesson.getSection().getCourse().getId();
        enrollmentService.markLessonCompleted(student.getEmail(), courseId, lesson.getId());
    }

    private LessonProgress findOrCreate(User student, Lesson lesson) {
        return progressRepository.findByStudentIdAndLessonId(student.getId(), lesson.getId())
                .orElseGet(() -> LessonProgress.builder()
                        .student(student)
                        .lesson(lesson)
                        .build());
    }

    private Lesson findLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leçon introuvable"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }
}
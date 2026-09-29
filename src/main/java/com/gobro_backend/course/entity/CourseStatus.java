package com.gobro_backend.course.entity;


/**
 * Course lifecycle, matching cahier des charges §13 (Administration ->
 * "Validation des cours"):
 *
 *  DRAFT              -> being authored by the instructor, not visible to anyone else
 *  PENDING_VALIDATION -> submitted by the instructor, awaiting admin review
 *  PUBLISHED          -> approved by an admin, visible in the public catalog
 *  REJECTED           -> rejected by an admin (see Course#rejectionReason)
 *  ARCHIVED           -> previously published, withdrawn from the catalog
 */
public enum CourseStatus {
    DRAFT,
    PENDING_VALIDATION,
    PUBLISHED,
    REJECTED,
    ARCHIVED
}
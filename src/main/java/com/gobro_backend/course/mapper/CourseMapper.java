package com.gobro_backend.course.mapper;


import com.gobro_backend.course.dto.CourseResponse;
import com.gobro_backend.course.dto.CreateCourseRequest;
import com.gobro_backend.course.entity.Course;
import com.gobro_backend.course.entity.CourseSection;
import com.gobro_backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true) // generated in CourseService from the title
    @Mapping(target = "status", ignore = true) // always starts DRAFT, set in CourseService
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "instructor", ignore = true) // set in CourseService from the authenticated user
    @Mapping(target = "sections", ignore = true)
    @Mapping(target = "totalDurationMinutes", constant = "0")
    @Mapping(target = "studentsCount", constant = "0")
    @Mapping(target = "averageRating", constant = "0.0")
    @Mapping(target = "reviewsCount", constant = "0")
    @Mapping(target = "publishedAt", ignore = true)
    Course toEntity(CreateCourseRequest request);

    /**
     * Applies only the non-null fields of an UpdateCourseRequest onto an
     * existing, already-loaded Course managed entity.
     */
    @org.mapstruct.BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "instructor", ignore = true)
    @Mapping(target = "sections", ignore = true)
    @Mapping(target = "totalDurationMinutes", ignore = true)
    @Mapping(target = "studentsCount", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewsCount", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    void updateEntityFromRequest(com.gobro_backend.course.dto.UpdateCourseRequest request, @MappingTarget Course course);

    @Mapping(target = "status", expression = "java(course.getStatus().name())")
    @Mapping(target = "instructor", source = "instructor")
    @Mapping(target = "sections", source = "sections")
    CourseResponse toResponse(Course course);

    List<CourseResponse> toResponseList(List<Course> courses);

    CourseResponse.InstructorSummary toInstructorSummary(User instructor);

    CourseResponse.SectionResponse toSectionResponse(CourseSection section);
}
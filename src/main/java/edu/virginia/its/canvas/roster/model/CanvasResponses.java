package edu.virginia.its.canvas.roster.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.ObjectUtils;

public final class CanvasResponses {

  public record Course(
      String id,
      String name,
      @JsonProperty("course_code") String courseCode,
      @JsonProperty("sis_course_id") String sisCourseId,
      Term term) {}

  public record Term(String id, String name, @JsonProperty("sis_term_id") String sisTermId) {}

  public record Section(
      String id,
      @JsonProperty("course_id") String courseId,
      String name,
      @JsonProperty("sis_section_id") String sisSectionId,
      @JsonProperty("sis_course_id") String sisCourseId,
      @JsonProperty("nonxlist_course_id") String crosslistedCourseId,
      @JsonProperty("total_students") int totalStudents) {
    public boolean isWaitlist() {
      return sisSectionId != null && sisSectionId.endsWith("_WL");
    }

    public boolean isCrosslisted() {
      return !ObjectUtils.isEmpty(crosslistedCourseId);
    }
  }

  public record Enrollment(
      String id,
      @JsonProperty("user_id") String userId,
      @JsonProperty("course_id") String courseId,
      String type,
      String role,
      @JsonProperty("enrollment_state") String enrollmentState,
      @JsonProperty("sis_course_id") String sisCourseId,
      @JsonProperty("sis_section_id") String sisSectionId,
      @JsonProperty("sis_user_id") String sisUserId) {}
}

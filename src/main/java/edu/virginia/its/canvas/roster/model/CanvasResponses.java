package edu.virginia.its.canvas.roster.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
      @JsonProperty("total_students") int totalStudents) {}
}

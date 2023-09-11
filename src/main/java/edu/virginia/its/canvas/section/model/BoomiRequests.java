package edu.virginia.its.canvas.section.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class BoomiRequests {
  public record CanvasWaitlistStatus(
      @JsonProperty("strm") String term,
      String subject,
      @JsonProperty("catalog_num") String catalogNumber,
      @JsonProperty("class_section") String classSection,
      @JsonProperty("acad_group") String academicGroup) {}

  public record CanvasWaitlist(
      @JsonProperty("strm") String term,
      String subject,
      @JsonProperty("catalog_num") String catalogNumber,
      @JsonProperty("class_section") String classSection,
      @JsonProperty("acad_group") String academicGroup,
      @JsonProperty Boolean waitlist) {}
}

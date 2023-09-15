package edu.virginia.its.canvas.section.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class BoomiResponses {

  public record SisSection(
      @JsonProperty("strm") String term,
      String subject,
      @JsonProperty("catalog_num") String catalogNumber,
      @JsonProperty("class_section") String classSection,
      @JsonProperty("acad_group") String academicGroup,
      @JsonProperty("waitlist") boolean hasWaitlist,
      @JsonProperty("total_students") int numberOfWaitlistStudents) {
    public String getSisSectionId() {
      return term + "_" + subject + "_" + catalogNumber + "-" + classSection + "_" + academicGroup;
    }
  }
}

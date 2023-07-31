package edu.virginia.its.canvas.section.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Class that takes a section string (such as '1228_PHYS_1050-001_CGAS') and splits it into the
 * relavant parts. Also will serialize into json into the format expected by boomi for SIS lookups
 * (the key names are based on the column names for the fields in the SIS DB).
 */
@Getter
public final class UvaSection {

  @JsonProperty("acad_group")
  private String group;

  @JsonProperty("subject")
  private String org;

  @JsonProperty("catalog_num")
  private String courseNumber;

  @JsonProperty("class_section")
  private String section;

  @JsonProperty("strm")
  private String term;

  @JsonProperty("waitlist")
  private Boolean waitlist;

  @JsonIgnore private final Boolean valid;

  public UvaSection(String section, Boolean waitlist) {
    // TODO: are there other format types we need to handle?
    String[] sectionParts = section.split("_");
    if (sectionParts.length == 4) {
      term = sectionParts[0];
      org = sectionParts[1];
      String[] courseParts = sectionParts[2].split("-");
      courseNumber = courseParts[0];
      this.section = courseParts[1];
      group = sectionParts[3];
      this.waitlist = waitlist;
      this.valid = true;
    } else {
      this.valid = false;
    }
  }
}

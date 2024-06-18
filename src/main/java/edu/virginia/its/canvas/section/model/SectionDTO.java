package edu.virginia.its.canvas.section.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SectionDTO {
  private String id;
  private String sisId;
  private String name;
  private String courseId;
  private String courseSisId;
  private TermDTO term;
  private int totalStudents;
  private boolean crosslisted;
  private String crosslistedCourseId;
  private boolean waitlist;
  private int waitlistStudents;
}

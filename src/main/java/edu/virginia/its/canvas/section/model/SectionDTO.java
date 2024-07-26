package edu.virginia.its.canvas.section.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class SectionDTO {
  private String id;
  private String sisId;
  private String name;
  private String courseId;
  private String courseSisId;
  @ToString.Exclude private TermDTO term;
  @ToString.Exclude private int totalStudents;
  private boolean crosslisted;
  private String crosslistedCourseId;
  private boolean waitlistDataFound;
  private boolean waitlist;
  private int waitlistStudents;
}

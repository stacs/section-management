package edu.virginia.its.canvas.section.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CanvasResponsesTest {

  @Test
  public void testIsCrosslisted() {
    CanvasResponses.Section section1 =
        new CanvasResponses.Section(
            "id", "courseId", "name", "sisSectionId", "sisCourseId", null, 1);
    CanvasResponses.Section section2 =
        new CanvasResponses.Section("id", "courseId", "name", "sisSectionId", "sisCourseId", "", 1);
    CanvasResponses.Section section3 =
        new CanvasResponses.Section(
            "id", "courseId", "name", "sisSectionId", "sisCourseId", "123", 1);
    assertFalse(section1.isCrosslisted());
    assertFalse(section2.isCrosslisted());
    assertTrue(section3.isCrosslisted());
  }

  @Test
  public void testIsWaitlist() {
    CanvasResponses.Section section1 =
        new CanvasResponses.Section(
            "id", "courseId", "name", null, "sisCourseId", "crosslistedCourseId", 1);
    CanvasResponses.Section section2 =
        new CanvasResponses.Section(
            "id", "courseId", "name", "sisSectionId", "sisCourseId", "crosslistedCourseId", 1);
    CanvasResponses.Section section3 =
        new CanvasResponses.Section(
            "id", "courseId", "name", "sisSectionId_WL", "sisCourseId", "crosslistedCourseId", 1);
    assertFalse(section1.isWaitlist());
    assertFalse(section2.isWaitlist());
    assertTrue(section3.isWaitlist());
  }
}

package edu.virginia.its.canvas.section.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import org.junit.jupiter.api.Test;

public class CanvasResponsesTest {

  @Test
  public void testIsCrosslisted() {
    CanvasSection canvasSection1 =
        new CanvasSection("id", "courseId", "name", "sisSectionId", "sisCourseId", null, 1);
    CanvasSection canvasSection2 =
        new CanvasSection("id", "courseId", "name", "sisSectionId", "sisCourseId", "", 1);
    CanvasSection canvasSection3 =
        new CanvasSection("id", "courseId", "name", "sisSectionId", "sisCourseId", "123", 1);
    assertFalse(canvasSection1.isCrosslisted());
    assertFalse(canvasSection2.isCrosslisted());
    assertTrue(canvasSection3.isCrosslisted());
  }
}

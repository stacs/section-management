package edu.virginia.its.canvas.section.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.virginia.its.canvas.section.model.CanvasResponses.Section;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SectionUtilsTest {

  @Test
  public void testGetSectionsCreatedInCourse() {
    Section section1 =
        new Section("id1", "courseId1", "name1", "sisSectionId1", "sisCourseId1", null, 1);
    Section section2 =
        new Section("id2", "courseId2", "name2", "sisSectionId2", "sisCourseId2", null, 2);
    Section section3 =
        new Section("id3", "courseId3", "name3", "sisSectionId3", "sisCourseId3", "courseId1", 3);
    List<Section> sections = new ArrayList<>(Arrays.asList(section1, section2, section3));
    List<Section> result = SectionUtils.getSectionsCreatedInCourse("courseId1", sections);
    assertEquals(2, result.size());
    assertTrue(result.contains(section1));
    assertTrue(result.contains(section3));
  }
}

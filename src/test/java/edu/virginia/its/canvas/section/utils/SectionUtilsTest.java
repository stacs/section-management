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

  @Test
  public void testSortSectionsByName() {
    List<Section> sections = new ArrayList<>();
    sections.add(new Section("4", "4", "My Section 4", "4", "4", "4", 1));
    sections.add(new Section("1", "1", "My Section 1", "1", "1", "1", 1));
    sections.add(new Section("2", "2", "My Section 2", "2", "2", "2", 1));
    sections.add(new Section("5", "5", "My Section 5", "5", "5", null, 1));
    sections.add(new Section("3", "3", "My Section 3", "3", "3", null, 1));

    sections.sort(SectionUtils.SECTION_NAME_COMPARATOR);

    assertEquals(5, sections.size());
    assertEquals("1", sections.get(0).id());
    assertEquals("2", sections.get(1).id());
    assertEquals("3", sections.get(2).id());
    assertEquals("4", sections.get(3).id());
    assertEquals("5", sections.get(4).id());
  }

  @Test
  public void testSortSectionsByCrosslistingThenName() {
    List<Section> sections = new ArrayList<>();
    sections.add(new Section("4", "4", "My Section 4", "4", "4", "4", 1));
    sections.add(new Section("1", "1", "My Section 1", "1", "1", "1", 1));
    sections.add(new Section("2", "2", "My Section 2", "2", "2", "2", 1));
    sections.add(new Section("5", "5", "My Section 5", "5", "5", null, 1));
    sections.add(new Section("3", "3", "My Section 3", "3", "3", null, 1));

    sections.sort(SectionUtils.ALREADY_ADDED_SECTIONS_COMPARATOR);

    assertEquals(5, sections.size());
    assertEquals("3", sections.get(0).id());
    assertEquals("5", sections.get(1).id());
    assertEquals("1", sections.get(2).id());
    assertEquals("2", sections.get(3).id());
    assertEquals("4", sections.get(4).id());
  }
}

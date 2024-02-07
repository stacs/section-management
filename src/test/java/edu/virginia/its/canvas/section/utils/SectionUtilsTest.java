package edu.virginia.its.canvas.section.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlist;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlistStatus;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SectionUtilsTest {

  @Test
  void testGetSectionsCreatedInCourse() {
    CanvasSection canvasSection1 =
        new CanvasSection("id1", "courseId1", "name1", "sisSectionId1", "sisCourseId1", null, 1);
    CanvasSection canvasSection2 =
        new CanvasSection("id2", "courseId2", "name2", "sisSectionId2", "sisCourseId2", null, 2);
    CanvasSection canvasSection3 =
        new CanvasSection(
            "id3", "courseId3", "name3", "sisSectionId3", "sisCourseId3", "courseId1", 3);
    List<CanvasSection> canvasSections =
        new ArrayList<>(Arrays.asList(canvasSection1, canvasSection2, canvasSection3));
    List<CanvasSection> result =
        SectionUtils.getSectionsCreatedInCourse("courseId1", canvasSections);
    assertEquals(2, result.size());
    assertTrue(result.contains(canvasSection1));
    assertTrue(result.contains(canvasSection3));
  }

  @Test
  void testSortSectionsByName() {
    List<CanvasSection> canvasSections = new ArrayList<>();
    canvasSections.add(new CanvasSection("4", "4", "My Section 4", "4", "4", "4", 1));
    canvasSections.add(new CanvasSection("1", "1", "My Section 1", "1", "1", "1", 1));
    canvasSections.add(new CanvasSection("2", "2", "My Section 2", "2", "2", "2", 1));
    canvasSections.add(new CanvasSection("5", "5", "My Section 5", "5", "5", null, 1));
    canvasSections.add(new CanvasSection("3", "3", "My Section 3", "3", "3", null, 1));

    canvasSections.sort(SectionUtils.SECTION_NAME_COMPARATOR);

    assertEquals(5, canvasSections.size());
    assertEquals("1", canvasSections.get(0).id());
    assertEquals("2", canvasSections.get(1).id());
    assertEquals("3", canvasSections.get(2).id());
    assertEquals("4", canvasSections.get(3).id());
    assertEquals("5", canvasSections.get(4).id());
  }

  @Test
  void testSortSectionsByCrosslistingThenName() {
    List<CanvasSection> canvasSections = new ArrayList<>();
    canvasSections.add(new CanvasSection("4", "4", "My Section 4", "4", "4", "4", 1));
    canvasSections.add(new CanvasSection("1", "1", "My Section 1", "1", "1", "1", 1));
    canvasSections.add(new CanvasSection("2", "2", "My Section 2", "2", "2", "2", 1));
    canvasSections.add(new CanvasSection("5", "5", "My Section 5", "5", "5", null, 1));
    canvasSections.add(new CanvasSection("3", "3", "My Section 3", "3", "3", null, 1));

    canvasSections.sort(SectionUtils.ALREADY_ADDED_SECTIONS_COMPARATOR);

    assertEquals(5, canvasSections.size());
    assertEquals("3", canvasSections.get(0).id());
    assertEquals("5", canvasSections.get(1).id());
    assertEquals("1", canvasSections.get(2).id());
    assertEquals("2", canvasSections.get(3).id());
    assertEquals("4", canvasSections.get(4).id());
  }

  @Test
  void testSectionIdToCanvasWaitlistStatus() {
    CanvasWaitlistStatus result =
        SectionUtils.sectionIdToCanvasWaitlistStatus("1228_PHYS_1050-001_CGAS");
    assertNotNull(result);
    assertEquals("1228", result.term());
    assertEquals("PHYS", result.subject());
    assertEquals("1050", result.catalogNumber());
    assertEquals("001", result.classSection());
    assertEquals("CGAS", result.academicGroup());

    result = SectionUtils.sectionIdToCanvasWaitlistStatus("testing bad input");
    assertNull(result);
  }

  @Test
  void testSectionIdToCanvasWaitlist() {
    CanvasWaitlist result = SectionUtils.sectionIdToCanvasWaitlist("1228_PHYS_1050-001_CGAS", true);
    assertNotNull(result);
    assertEquals("1228", result.term());
    assertEquals("PHYS", result.subject());
    assertEquals("1050", result.catalogNumber());
    assertEquals("001", result.classSection());
    assertEquals("CGAS", result.academicGroup());
    assertTrue(result.waitlist());

    result = SectionUtils.sectionIdToCanvasWaitlist("testing bad input", false);
    assertNull(result);
  }
}

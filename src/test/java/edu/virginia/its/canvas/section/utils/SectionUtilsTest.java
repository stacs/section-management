package edu.virginia.its.canvas.section.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlist;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlistStatus;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.SectionDTO;
import edu.virginia.its.canvas.section.model.TermDTO;
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
    List<SectionDTO> sectionDTOS = new ArrayList<>();
    sectionDTOS.add(
        new SectionDTO(
            "4", "4", "My SectionDTO 4", "4", "4", new TermDTO(), 4, true, "4", false, false, 0));
    sectionDTOS.add(
        new SectionDTO(
            "1", "1", "My SectionDTO 1", "1", "1", new TermDTO(), 1, true, "1", false, false, 0));
    sectionDTOS.add(
        new SectionDTO(
            "2", "2", "My SectionDTO 2", "2", "2", new TermDTO(), 2, true, "2", false, false, 0));
    sectionDTOS.add(
        new SectionDTO(
            "5", "5", "My SectionDTO 5", "5", "5", new TermDTO(), 5, false, "", false, false, 0));
    sectionDTOS.add(
        new SectionDTO(
            "3", "3", "My SectionDTO 3", "3", "3", new TermDTO(), 3, false, "", false, false, 0));

    sectionDTOS.sort(SectionUtils.SECTION_NAME_COMPARATOR);

    assertEquals(5, sectionDTOS.size());
    assertEquals("1", sectionDTOS.get(0).getId());
    assertEquals("2", sectionDTOS.get(1).getId());
    assertEquals("3", sectionDTOS.get(2).getId());
    assertEquals("4", sectionDTOS.get(3).getId());
    assertEquals("5", sectionDTOS.get(4).getId());
  }

  @Test
  void testSortSectionsByCrosslistingThenName() {
    List<SectionDTO> sectionDTOS = new ArrayList<>();
    sectionDTOS.add(
        new SectionDTO(
            "4", "4", "My SectionDTO 4", "4", "4", new TermDTO(), 4, true, "4", false, false, 0));
    sectionDTOS.add(
        new SectionDTO(
            "1", "1", "My SectionDTO 1", "1", "1", new TermDTO(), 1, true, "1", false, false, 0));
    sectionDTOS.add(
        new SectionDTO(
            "2", "2", "My SectionDTO 2", "2", "2", new TermDTO(), 2, true, "2", false, false, 0));
    sectionDTOS.add(
        new SectionDTO(
            "5", "5", "My SectionDTO 5", "5", "5", new TermDTO(), 5, false, "", false, false, 0));
    sectionDTOS.add(
        new SectionDTO(
            "3", "3", "My SectionDTO 3", "3", "3", new TermDTO(), 3, false, "", false, false, 0));

    sectionDTOS.sort(SectionUtils.ALREADY_ADDED_SECTIONS_COMPARATOR);

    assertEquals(5, sectionDTOS.size());
    assertEquals("3", sectionDTOS.get(0).getId());
    assertEquals("5", sectionDTOS.get(1).getId());
    assertEquals("1", sectionDTOS.get(2).getId());
    assertEquals("2", sectionDTOS.get(3).getId());
    assertEquals("4", sectionDTOS.get(4).getId());
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

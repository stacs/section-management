package edu.virginia.its.canvas.roster.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.virginia.its.canvas.roster.RosterManagementApplication;
import edu.virginia.its.canvas.roster.SecurityConfig;
import edu.virginia.its.canvas.roster.api.CanvasApi;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Section;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {RosterManagementApplication.class, SecurityConfig.class})
public class RosterManagementServiceTest {

  @Test
  public void testGetSectionsCreatedInCourse() {
    RosterManagementService service = new RosterManagementService(mock(CanvasApi.class));
    Section section1 =
        new Section("id1", "courseId1", "name1", "sisSectionId1", "sisCourseId1", null, 1);
    Section section2 =
        new Section("id2", "courseId2", "name2", "sisSectionId2", "sisCourseId2", null, 2);
    Section section3 =
        new Section("id3", "courseId3", "name3", "sisSectionId3", "sisCourseId3", "courseId1", 3);
    List<Section> sections = new ArrayList<>(Arrays.asList(section1, section2, section3));
    List<Section> result = service.getSectionsCreatedInCourse("courseId1", sections);
    assertEquals(2, result.size());
    assertTrue(result.contains(section1));
    assertTrue(result.contains(section3));
  }

  @Test
  public void testGetValidCourseSections() {
    List<Section> sections = new ArrayList<>();
    sections.add(new Section("4", "4", "My Section 4", "4", "4", "4", 1));
    sections.add(new Section("1", "1", "My Section 1", "1", "1", "1", 1));
    sections.add(new Section("2", "2", "My Section 2", "2", "2", "2", 1));
    sections.add(new Section("5", "5", "My Section 5", "5", "5", null, 1));
    sections.add(new Section("3", "3", "My Section 3", "3", "3", null, 1));
    CanvasApi api = mock(CanvasApi.class);
    when(api.getCourseSections("123")).thenReturn(sections);
    RosterManagementService service = new RosterManagementService(api);

    List<Section> result = service.getValidCourseSections("123");
    assertEquals(5, result.size());
    assertEquals("3", result.get(0).id());
    assertEquals("5", result.get(1).id());
    assertEquals("1", result.get(2).id());
    assertEquals("2", result.get(3).id());
    assertEquals("4", result.get(4).id());
  }
}

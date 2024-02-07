package edu.virginia.its.canvas.section.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.virginia.its.canvas.section.SectionManagementApplication;
import edu.virginia.its.canvas.section.SecurityConfig;
import edu.virginia.its.canvas.section.api.CanvasApi;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {SectionManagementApplication.class, SecurityConfig.class})
class SectionManagementServiceTest {

  @Test
  void testGetValidCourseSections() {
    List<CanvasSection> canvasSections = new ArrayList<>();
    canvasSections.add(new CanvasSection("4", "4", "My Section 4", "4", "4", "4", 1));
    canvasSections.add(new CanvasSection("1", "1", "My Section 1", "1", "1", "1", 1));
    canvasSections.add(new CanvasSection("2", "2", "My Section 2", null, "2", "2", 1));
    canvasSections.add(new CanvasSection("5", "5", "My Section 5", "5", "5", null, 1));
    canvasSections.add(new CanvasSection("3", "3", "My Section 3", null, "3", null, 1));
    CanvasApi api = mock(CanvasApi.class);
    when(api.getCourseSections("123")).thenReturn(canvasSections);
    SectionManagementService service = new SectionManagementService(api);

    List<CanvasSection> result = service.getValidCourseSections("123");
    assertEquals(3, result.size());
    assertEquals("1", result.get(0).id());
    assertEquals("4", result.get(1).id());
    assertEquals("5", result.get(2).id());
  }
}

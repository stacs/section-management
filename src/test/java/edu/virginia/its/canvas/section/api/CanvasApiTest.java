package edu.virginia.its.canvas.section.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.virginia.its.canvas.lti.util.Constants;
import edu.virginia.its.canvas.section.SectionManagementApplication;
import edu.virginia.its.canvas.section.SecurityConfig;
import edu.virginia.its.canvas.section.model.CanvasResponses;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.Link;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.StreamUtils;

@SpringBootTest
@ContextConfiguration(classes = {SectionManagementApplication.class, SecurityConfig.class})
class CanvasApiTest {

  public MockWebServer mockBackEnd;

  private CanvasApi canvasApi;

  @BeforeEach
  public void init() throws IOException {
    mockBackEnd = new MockWebServer();
    mockBackEnd.start();
    String baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());
    canvasApi = new CanvasApi(baseUrl, "token", 15);
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockBackEnd.shutdown();
  }

  @Test
  void testGetCourse() throws Exception {
    String json =
        StreamUtils.copyToString(
            new ClassPathResource("canvasCourse.json").getInputStream(), Charset.defaultCharset());
    mockBackEnd.enqueue(
        new MockResponse().setBody(json).addHeader("Content-Type", "application/json"));
    CanvasResponses.Course course = canvasApi.getCourse("1");
    assertEquals("My Awesome Canvas Course", course.name());
    assertEquals("my-sis-course", course.sisCourseId());
    assertNotNull(course.term());
    assertEquals("1", course.term().id());
    assertEquals("Spring 2023", course.term().name());
    assertEquals("1232", course.term().sisTermId());
    RecordedRequest request = mockBackEnd.takeRequest();
    assertEquals("/courses/1?include%5B%5D=term", request.getPath());
  }

  @Test
  void testGetUsersTeachingCourses_paging() throws Exception {
    String json1 =
        StreamUtils.copyToString(
            new ClassPathResource("canvasCourse1.json").getInputStream(), Charset.defaultCharset());
    String json2 =
        StreamUtils.copyToString(
            new ClassPathResource("canvasCourse2.json").getInputStream(), Charset.defaultCharset());
    String json3 =
        StreamUtils.copyToString(
            new ClassPathResource("canvasCourse3.json").getInputStream(), Charset.defaultCharset());
    HttpUrl linkUrl = mockBackEnd.url("/paging");
    mockBackEnd.enqueue(
        new MockResponse()
            .setBody(json1)
            .addHeader("Content-Type", "application/json")
            .addHeader(Constants.CANVAS_LINK_HEADER, Link.of(linkUrl + "-1", "next").toString()));
    mockBackEnd.enqueue(
        new MockResponse().setBody(json2).addHeader("Content-Type", "application/json"));
    mockBackEnd.enqueue(
        new MockResponse().setBody(json3).addHeader("Content-Type", "application/json"));
    List<CanvasResponses.Course> usersTeachingCourses =
        canvasApi.getUsersTeachingCourses("test-user");
    assertEquals(3, usersTeachingCourses.size());
    CanvasResponses.Course course =
        usersTeachingCourses.stream().filter(c -> "1".equals(c.id())).findFirst().orElse(null);
    assertNotNull(course);
    RecordedRequest request = mockBackEnd.takeRequest();
    assertEquals(
        "/users/sis_user_id:test-user/courses?per_page=100&include%5B%5D=term&enrollment_type=Teacher",
        request.getPath());
    request = mockBackEnd.takeRequest();
    assertEquals("/paging-1", request.getPath());
    request = mockBackEnd.takeRequest();
    assertEquals(
        "/users/sis_user_id:test-user/courses?per_page=100&include%5B%5D=term&enrollment_type=TA",
        request.getPath());
  }
}

package edu.virginia.its.canvas.section.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

import com.nimbusds.jose.shaded.json.JSONObject;
import edu.virginia.its.canvas.lti.util.CanvasAuthenticationToken;
import edu.virginia.its.canvas.lti.util.Constants;
import edu.virginia.its.canvas.section.SectionManagementApplication;
import edu.virginia.its.canvas.section.SecurityConfig;
import edu.virginia.its.canvas.section.model.BoomiResponses.SisSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.Course;
import edu.virginia.its.canvas.section.model.CanvasResponses.Term;
import edu.virginia.its.canvas.section.service.SectionManagementService;
import edu.virginia.its.canvas.section.service.WaitlistedSectionService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.lti13.lti.Claims;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {SectionManagementApplication.class, SecurityConfig.class})
class SectionManagementControllerTest {
  @MockBean private SectionManagementService sectionManagementService;
  @MockBean private WaitlistedSectionService waitlistedSectionService;

  @Autowired private MockMvc mockMvc;

  @Test
  void testController_noAuth() throws Exception {
    this.mockMvc.perform(get("/index")).andExpect(status().isForbidden());
  }

  @Test
  void testConfigController_noAuth() throws Exception {
    this.mockMvc.perform(get("/config.json")).andExpect(status().isOk());
  }

  @Test
  void testController_userRole() throws Exception {
    List<String> roles = new ArrayList<>();
    roles.add("ROLE_USER");
    SecurityContextHolder.getContext().setAuthentication(getToken(roles, "user", "123"));
    this.mockMvc.perform(get("/index")).andExpect(status().isForbidden());
  }

  @Test
  void testController_studentRole() throws Exception {
    List<String> roles = new ArrayList<>();
    roles.add("ROLE_STUDENT");
    SecurityContextHolder.getContext().setAuthentication(getToken(roles, "user", "123"));
    this.mockMvc.perform(get("/index")).andExpect(status().isForbidden());
  }

  @Test
  void testController_instructorRole() throws Exception {
    List<String> roles = new ArrayList<>();
    roles.add("ROLE_INSTRUCTOR");
    SecurityContextHolder.getContext().setAuthentication(getToken(roles, "user", "123"));
    Term term = new Term("sis-term-id", "Test Term", "sis-term-id");
    Course course = new Course("123", "My Awesome Course", "My Course Code", "sis-course-id", term);
    List<Course> courseList = new ArrayList<>();
    courseList.add(course);
    List<CanvasSection> canvasSections = new ArrayList<>();
    CanvasSection canvasSection1 =
        new CanvasSection("1", "123", "Section 1", "sis-section-id-1", "sis-course-id", null, 1);
    CanvasSection canvasSection2 =
        new CanvasSection("2", "123", "Section 2", "sis-section-id-2", "sis-course-id", "321", 2);
    CanvasSection canvasSection3 =
        new CanvasSection("3", "123", "Section 3", "sis-section-id-3", "sis-course-id", null, 3);
    canvasSections.add(canvasSection1);
    canvasSections.add(canvasSection2);
    canvasSections.add(canvasSection3);
    List<SisSection> sisSections = new ArrayList<>();
    SisSection sisSection1 =
        new SisSection(
            "term", "subject", "catalogNumber", "classSection", "academicGroup", true, 4);
    SisSection sisSection2 =
        new SisSection(
            "term", "subject", "catalogNumber", "classSection", "academicGroup", false, 0);
    sisSections.add(sisSection1);
    sisSections.add(sisSection2);
    Map<Term, List<CanvasSection>> termMap = new HashMap<>();
    List<CanvasSection> allUserSections = new ArrayList<>(canvasSections);
    CanvasSection canvasSection4 =
        new CanvasSection("4", "321", "Section 4", "sis-section-id-4", "sis-course-id-2", null, 4);
    allUserSections.add(canvasSection4);
    termMap.put(term, allUserSections);

    when(sectionManagementService.getCourse("123")).thenReturn(course);
    when(sectionManagementService.getValidCourseSections("123")).thenReturn(canvasSections);
    when(waitlistedSectionService.getWaitlistStatusForSections(canvasSections))
        .thenReturn(sisSections);
    when(sectionManagementService.getUserCourses("user")).thenReturn(courseList);
    when(sectionManagementService.getAllUserSectionsGroupedByTerm(courseList)).thenReturn(termMap);
    this.mockMvc
        .perform(get("/index"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(model().attribute("courseName", course.name()))
        .andExpect(model().attribute("courseCode", course.courseCode()))
        .andExpect(model().attributeExists("currentCourseSections"))
        .andExpect(model().attribute("currentCourseSections", hasSize(3)))
        .andExpect(
            model()
                .attribute(
                    "currentCourseSections",
                    containsInRelativeOrder(canvasSection1, canvasSection3, canvasSection2)))
        .andExpect(model().attributeExists("waitlistSectionsMap"))
        .andExpect(model().attribute("waitlistSectionsMap", aMapWithSize(1)))
        .andExpect(
            model()
                .attribute(
                    "waitlistSectionsMap",
                    hasEntry("term_subject_catalogNumber-classSection_academicGroup", sisSection1)))
        .andExpect(model().attributeExists("sectionsMap"))
        .andExpect(model().attribute("sectionsMap", aMapWithSize(1)))
        .andExpect(model().attribute("sectionsMap", hasEntry(term, List.of(canvasSection4))))
        .andExpect(model().attributeExists("sectionManagementForm"))
        .andExpect(
            model()
                .attribute(
                    "sectionManagementForm",
                    hasProperty("sectionsToKeep", contains("1", "3", "2"))));
  }

  private CanvasAuthenticationToken getToken(List<String> roles, String username, String courseId) {
    String nameAttributeKey = "sub";
    Map<String, Object> attributes = getAttributes(nameAttributeKey, username, courseId);
    OAuth2User user =
        new DefaultOAuth2User(
            AuthorityUtils.createAuthorityList("ROLE_USER"), attributes, nameAttributeKey);
    roles.add("ROLE_USER");
    OidcAuthenticationToken oidcAuthenticationToken =
        new OidcAuthenticationToken(
            user,
            AuthorityUtils.createAuthorityList(roles.toArray(String[]::new)),
            "authorizedClientRegistrationId",
            "state");
    return new CanvasAuthenticationToken(oidcAuthenticationToken);
  }

  private Map<String, Object> getAttributes(
      String nameAttributeKey, String username, String courseId) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(nameAttributeKey, Objects.requireNonNullElse(username, "username"));
    attributes.put("https://www.instructure.com/placement", "myPlacement");
    attributes.put("email", "myEmail");
    attributes.put("name", "myName");
    attributes.put("given_name", "myGivenName");
    attributes.put("family_name", "myFamilyName");
    attributes.put("picture", "myPicture");
    attributes.put("locale", "en");
    JSONObject customFields = new JSONObject();
    if (courseId != null) {
      customFields.put(Constants.COURSE_ID_CUSTOM_KEY, courseId);
    }
    if (username != null) {
      customFields.put(Constants.USERNAME_CUSTOM_KEY, username);
    }
    if (!customFields.isEmpty()) {
      attributes.put(Claims.CUSTOM, customFields);
    }
    return attributes;
  }
}

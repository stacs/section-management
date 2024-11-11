package edu.virginia.its.canvas.section.api;

import edu.virginia.its.canvas.lti.util.Constants;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.Course;
import edu.virginia.its.canvas.section.model.CanvasResponses.Enrollment;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CanvasApi {

  private final WebClient canvasApi;
  private final String canvasAuthorization;
  private final Duration requestTimeout;

  private static final String INCLUDE = "include[]";
  private static final String PER_PAGE = "per_page";
  private static final String AUTHORIZATION = "Authorization";

  public CanvasApi(
      @Value("${ltitool.canvas.apiUrl}") String canvasApiUrl,
      @Value("${ltitool.canvas.apiToken}") String canvasApiToken,
      @Value("${ltitool.canvas.apiTimeout:15}") Integer canvasApiTimeout) {
    canvasApi = WebClient.builder().baseUrl(canvasApiUrl).build();
    canvasAuthorization = "Bearer " + canvasApiToken;
    requestTimeout = Duration.ofSeconds(canvasApiTimeout);
    log.info("Canvas API URL: {}", canvasApiUrl);
    log.info("Canvas API Timeout: {}", canvasApiTimeout);
  }

  public Course getCourse(String courseId) {
    return canvasApi
        .get()
        .uri(
            uriBuilder ->
                uriBuilder.path("/courses/{id}").queryParam(INCLUDE, "term").build(courseId))
        .header(AUTHORIZATION, canvasAuthorization)
        .retrieve()
        .bodyToMono(Course.class)
        .block(requestTimeout);
  }

  public List<Course> getUsersTeachingCourses(String computingId) {
    // The courses API endpoint has an option to return section data, but it doesn't return the
    // section's SIS ID
    // (assuming the section has one) so we have to call the sections API endpoint separately.
    String uri =
        UriComponentsBuilder.fromPath("/users/sis_user_id:{computingId}/courses")
            .queryParam(PER_PAGE, "100")
            .queryParam(INCLUDE, "term")
            .queryParam("enrollment_type", "Teacher")
            // Using buildAndExpand() instead of just build() will stop 'include[]' from being
            // double url encoded.
            .buildAndExpand(computingId)
            .toString();
    List<Course> results = new ArrayList<>();
    getPagedResponses(uri, Course[].class, results);
    uri =
        UriComponentsBuilder.fromPath("/users/sis_user_id:{computingId}/courses")
            .queryParam(PER_PAGE, "100")
            .queryParam(INCLUDE, "term")
            .queryParam("enrollment_type", "TA")
            // Using buildAndExpand() instead of just build() will stop 'include[]' from being
            // double url encoded.
            .buildAndExpand(computingId)
            .toString();
    getPagedResponses(uri, Course[].class, results);
    return results;
  }

  public List<CanvasSection> getCourseSections(String courseId) {
    String uri =
        UriComponentsBuilder.fromPath("/courses/{id}/sections")
            .queryParam(PER_PAGE, "100")
            .queryParam(INCLUDE, "total_students")
            .buildAndExpand(courseId)
            .toString();
    List<CanvasSection> results = new ArrayList<>();
    getPagedResponses(uri, CanvasSection[].class, results);
    return results;
  }

  public CanvasSection crosslistSection(String sectionId, String newCourseId) {
    return canvasApi
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/sections/{sectionId}/crosslist/{newCourseId}")
                    .build(sectionId, newCourseId))
        .header(AUTHORIZATION, canvasAuthorization)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            response -> {
              log.warn(
                  "Error crosslisting section '{}' into course '{}', status code given was '{}'",
                  sectionId,
                  newCourseId,
                  response.statusCode());
              return Mono.error(new Exception("Could not crosslist section"));
            })
        .bodyToMono(CanvasSection.class)
        .block(requestTimeout);
  }

  public CanvasSection deCrosslistSection(String sectionId) {
    return canvasApi
        .delete()
        .uri(uriBuilder -> uriBuilder.path("/sections/{sectionId}/crosslist").build(sectionId))
        .header(AUTHORIZATION, canvasAuthorization)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            response -> {
              log.warn(
                  "Error de-crosslisting section '{}', status code given was '{}'",
                  sectionId,
                  response.statusCode());
              return Mono.error(new Exception("Could not de-crosslist section"));
            })
        .bodyToMono(CanvasSection.class)
        .block(requestTimeout);
  }

  public List<Enrollment> getCourseEnrollments(String courseId) {
    String uri =
        UriComponentsBuilder.fromPath("/courses/{id}/enrollments")
            .queryParam(PER_PAGE, "100")
            .build(courseId)
            .toString();
    List<Enrollment> results = new ArrayList<>();
    getPagedResponses(uri, Enrollment[].class, results);
    return results;
  }

  public Enrollment deleteUserEnrollment(String courseId, String enrollmentId) {
    return canvasApi
        .delete()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/courses/{courseId}/enrollments/{enrollmentId}")
                    .queryParam("task", "delete")
                    .build(courseId, enrollmentId))
        .header(AUTHORIZATION, canvasAuthorization)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            response -> {
              log.warn(
                  "Error removing enrollment '{}' from course '{}', status code given was '{}'",
                  enrollmentId,
                  courseId,
                  response.statusCode());
              return Mono.error(new Exception("Could not remove enrollment from course"));
            })
        .bodyToMono(Enrollment.class)
        .block(requestTimeout);
  }

  private <T> void getPagedResponses(String uri, Class<T[]> objectClass, List<T> results) {
    ResponseEntity<T[]> response =
        canvasApi
            .get()
            .uri(uri)
            .header(AUTHORIZATION, canvasAuthorization)
            .retrieve()
            .toEntity(objectClass)
            .block(requestTimeout);
    if (response != null && response.getBody() != null) {
      results.addAll(Arrays.asList(response.getBody()));
      String nextPageLink = getNextPageLink(response.getHeaders());
      if (nextPageLink != null) {
        getPagedResponses(nextPageLink, objectClass, results);
      }
    }
  }

  private String getNextPageLink(HttpHeaders headers) {
    String linkString = headers.getFirst(Constants.CANVAS_LINK_HEADER);
    if (linkString != null && !linkString.isEmpty()) {
      String[] links = linkString.split(",");
      for (String link : links) {
        Link linkObject = Link.valueOf(link);
        if (linkObject.hasRel("next")) {
          String href = linkObject.getHref();
          // We need to decode the href, otherwise query params such as 'include[]' won't work
          return URLDecoder.decode(href, StandardCharsets.UTF_8);
        }
      }
    }
    return null;
  }
}

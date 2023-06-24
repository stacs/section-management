package edu.virginia.its.canvas.roster.api;

import edu.virginia.its.canvas.roster.model.CanvasResponses.Course;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Section;
import edu.virginia.its.canvas.roster.utils.Constants;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

  public CanvasApi(
      @Value("${ltitool.canvas.apiUrl}") String canvasApiUrl,
      @Value("${ltitool.canvas.apiToken}") String canvasApiToken,
      @Value("${ltitool.canvas.apiTimeout:15}") Integer canvasApiTimeout) {
    canvasApi = WebClient.builder().baseUrl(canvasApiUrl).build();
    canvasAuthorization = "Bearer " + canvasApiToken;
    requestTimeout = Duration.ofSeconds(canvasApiTimeout);
  }

  public Course getCourse(String courseId) {
    return canvasApi
        .get()
        .uri(
            uriBuilder ->
                uriBuilder.path("/courses/{id}").queryParam("include[]", "term").build(courseId))
        .header("Authorization", canvasAuthorization)
        .retrieve()
        .bodyToMono(Course.class)
        .block(requestTimeout);
  }

  public List<Course> getUserCourses(String computingId) {
    // The courses API endpoint has an option to return section data, but it doesn't return the
    // section's SIS ID
    // (assuming the section has one) so we have to call the sections API endpoint separately.
    String uri =
        UriComponentsBuilder.fromPath("/users/sis_user_id:{computingId}/courses")
            .queryParam("per_page", "100")
            .queryParam("include[]", "term")
            .queryParam("enrollment_type", "Teacher")
            // Using buildAndExpand() instead of just build() will stop 'include[]' from being
            // double url encoded.
            .buildAndExpand(computingId)
            .toString();
    List<Course> results = new ArrayList<>();
    getPagedResponses(uri, Course[].class, results);
    return results;
  }

  public List<Section> getCourseSections(String courseId) {
    String uri =
        UriComponentsBuilder.fromPath("/courses/{id}/sections")
            .queryParam("per_page", "100")
            .queryParam("include[]", "total_students")
            .buildAndExpand(courseId)
            .toString();
    List<Section> results = new ArrayList<>();
    getPagedResponses(uri, Section[].class, results);
    return results;
  }

  public Section crosslistSection(String sectionId, String newCourseId) {
    return canvasApi
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/sections/{sectionId}/crosslist/{newCourseId}")
                    .build(sectionId, newCourseId))
        .header("Authorization", canvasAuthorization)
        .retrieve()
        .onStatus(
            HttpStatus::isError,
            response -> {
              log.warn(
                  "Error crosslisting section '{}' into course '{}', status code given was '{}'",
                  sectionId,
                  newCourseId,
                  response.statusCode());
              return Mono.error(new Exception("Could not crosslist section"));
            })
        .bodyToMono(Section.class)
        .block(requestTimeout);
  }

  public Section deCrosslistSection(String sectionId) {
    return canvasApi
        .delete()
        .uri(uriBuilder -> uriBuilder.path("/sections/{sectionId}/crosslist").build(sectionId))
        .header("Authorization", canvasAuthorization)
        .retrieve()
        .onStatus(
            HttpStatus::isError,
            response -> {
              log.warn(
                  "Error de-crosslisting section '{}', status code given was '{}'",
                  sectionId,
                  response.statusCode());
              return Mono.error(new Exception("Could not de-crosslist section"));
            })
        .bodyToMono(Section.class)
        .block(requestTimeout);
  }

  private <T> void getPagedResponses(String uri, Class<T[]> objectClass, List<T> results) {
    ResponseEntity<T[]> response =
        canvasApi
            .get()
            .uri(uri)
            .header("Authorization", canvasAuthorization)
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
          return linkObject.getHref();
        }
      }
    }
    return null;
  }
}

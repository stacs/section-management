package edu.virginia.its.canvas.section.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlist;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlistStatus;
import edu.virginia.its.canvas.section.model.BoomiResponses.SisSection;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class BoomiApi {

  private final Logger log = LoggerFactory.getLogger(BoomiApi.class);

  @Value("${ltitool.boomi.apiToken}")
  private String boomiApiToken;

  private final WebClient boomiApi;
  private final Duration requestTimeout;
  private final ObjectMapper objectMapper;

  @Autowired
  public BoomiApi(
      @Value("${ltitool.boomi.url}") String boomiUrl,
      @Value("${ltitool.boomi.apiTimeout:15}") Integer boomiApiTimeout) {
    boomiApi = WebClient.create(boomiUrl);
    requestTimeout = Duration.ofSeconds(boomiApiTimeout);
    objectMapper = new ObjectMapper();
    // Boomi expects the waitlist Boolean to be a String
    objectMapper.configOverride(Boolean.class).setFormat(JsonFormat.Value.forShape(Shape.STRING));
    log.info("Boomi API URL: {}", boomiUrl);
    log.info("Boomi API Timeout: {}", boomiApiTimeout);
  }

  public List<SisSection> getWaitlistStatusForSections(List<CanvasWaitlistStatus> sections) {
    if (!sections.isEmpty()) {
      ObjectNode body = objectMapper.createObjectNode();
      ArrayNode waitlists = objectMapper.valueToTree(sections);
      body.set("classes", waitlists);
      return boomiApi
          .post()
          .uri("ws/rest/uvacanvas/canvasWaitlistStatus")
          .header("X-API-KEY", boomiApiToken)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(JsonNode.class)
          .map(s -> s.path("waitlists"))
          .map(
              s -> {
                try {
                  return new ObjectMapper()
                      .readValue(s.traverse(), new TypeReference<List<SisSection>>() {});
                } catch (IOException e) {
                  log.error("Error while deserializing waitlist sections json", e);
                  return new ArrayList<SisSection>();
                }
              })
          .block(requestTimeout);
    } else {
      return List.of();
    }
  }

  public boolean updateWaitlistsForSections(List<CanvasWaitlist> sections) {
    if (!sections.isEmpty()) {
      ObjectNode body = objectMapper.createObjectNode();
      ArrayNode waitlists = objectMapper.valueToTree(sections);
      body.set("waitlists", waitlists);
      ClientResponse response =
          boomiApi
              .post()
              .uri("ws/rest/uvacanvas/canvasWaitlist")
              .header("X-API-KEY", boomiApiToken)
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(body)
              .exchange()
              .block(requestTimeout);
      if (response != null) {
        if (HttpStatus.OK.equals(response.statusCode())) {
          return true;
        } else {
          log.error(
              "Received a '{}' response from server with JSON '{}' when attempting to set SIS waitlisted sections",
              response.statusCode(),
              response.bodyToMono(String.class));
        }
      } else {
        log.error(
            "Received no response from server when attempting to set SIS waitlisted sections");
      }
    } else {
      log.warn("Waitlist sections list to send to SIS was empty");
    }
    return false;
  }
}

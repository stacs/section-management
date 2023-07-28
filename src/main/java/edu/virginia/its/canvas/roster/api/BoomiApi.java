package edu.virginia.its.canvas.roster.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.virginia.its.canvas.roster.model.UvaSection;
import edu.virginia.its.canvas.roster.model.WaitlistedSection;
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
  }

  public boolean updateWaitlistsForSections(List<WaitlistedSection> waitlistedSections) {
    List<UvaSection> uvaSectionWaitlists = new ArrayList<>();
    for (WaitlistedSection waitlistedSection : waitlistedSections) {
      UvaSection section =
          new UvaSection(waitlistedSection.getSisSectionId(), waitlistedSection.isWaitlisted());
      // Make sure the section given is valid vs something like one of our test sections which don't
      // use valid names (1226_CIS_1171-1_UNKX_261170101)
      if (section.getValid()) {
        uvaSectionWaitlists.add(section);
      }
    }
    if (!uvaSectionWaitlists.isEmpty()) {
      ObjectNode body = objectMapper.createObjectNode();
      ArrayNode waitlists = objectMapper.valueToTree(uvaSectionWaitlists);
      log.info("Sending the following request to Boomi: {}", waitlists.toPrettyString());
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
      return false;
    } else {
      log.warn(
          "Could not find any valid sections to send to Boomi for waitlist updates: {}",
          waitlistedSections);
      return false;
    }
  }
}

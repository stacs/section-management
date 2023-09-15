package edu.virginia.its.canvas.section.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlistStatus;
import edu.virginia.its.canvas.section.model.BoomiResponses.SisSection;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

@SpringBootTest
public class BoomiApiTest {
  public MockWebServer mockBackEnd;

  private BoomiApi boomiApi;

  @BeforeEach
  public void init() throws IOException {
    mockBackEnd = new MockWebServer();
    mockBackEnd.start();
    String baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());
    boomiApi = new BoomiApi(baseUrl, 15);
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockBackEnd.shutdown();
  }

  @Test
  public void testGetWaitlistStatusForSections() throws Exception {
    String json =
        StreamUtils.copyToString(
            new ClassPathResource("waitlistStatus.json").getInputStream(),
            Charset.defaultCharset());
    mockBackEnd.enqueue(
        new MockResponse().setBody(json).addHeader("Content-Type", "application/json"));
    List<SisSection> waitlistStatusList =
        boomiApi.getWaitlistStatusForSections(
            List.of(new CanvasWaitlistStatus("1238", "AAS", "1010", "100", "CGAS")));
    assertEquals(1, waitlistStatusList.size());
    SisSection sisSection = waitlistStatusList.get(0);
    assertNotNull(sisSection);
    assertEquals("CGAS", sisSection.academicGroup());
    assertEquals("AAS", sisSection.subject());
    assertEquals("1010", sisSection.catalogNumber());
    assertEquals("100", sisSection.classSection());
    assertEquals("1238", sisSection.term());
    assertFalse(sisSection.hasWaitlist());
    assertEquals("1238_AAS_1010-100_CGAS", sisSection.getSisSectionId());
  }
}

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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;

@SpringBootTest
class BoomiApiTest {
  static MockWebServer mockBackEnd;

  @Autowired private BoomiApi boomiApi;

  @Autowired Environment environment;

  @DynamicPropertySource
  static void setupUrl(DynamicPropertyRegistry registry) {
    registry.add(
        "ltitool.boomi.url", () -> String.format("http://localhost:%s", mockBackEnd.getPort()));
  }

  @BeforeAll
  static void init() throws IOException {
    mockBackEnd = new MockWebServer();
    mockBackEnd.start();
  }

  @AfterAll
  static void tearDown() throws IOException {
    mockBackEnd.shutdown();
  }

  @Test
  void testGetWaitlistStatusForSections() throws Exception {
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
    assertEquals("1234!#{aBCd", mockBackEnd.takeRequest().getHeader("X-API-KEY"));
  }
}

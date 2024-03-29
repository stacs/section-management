package edu.virginia.its.canvas.section.service;

import edu.virginia.its.canvas.section.api.BoomiApi;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlist;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlistStatus;
import edu.virginia.its.canvas.section.model.BoomiResponses.SisSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.utils.SectionUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WaitlistedSectionService {

  private final BoomiApi boomiApi;

  public WaitlistedSectionService(BoomiApi boomiApi) {
    this.boomiApi = boomiApi;
  }

  public void addWaitlistSections(String computingId, List<SisSection> sisSections) {
    List<CanvasWaitlist> sectionsList = new ArrayList<>();
    for (SisSection sisSection : sisSections) {
      CanvasWaitlist section =
          SectionUtils.sectionIdToCanvasWaitlist(sisSection.getSisSectionId(), true);
      if (section != null) {
        sectionsList.add(section);
      }
    }
    log.info(
        "User '{}' is enabling the following sections for waitlists: {}", computingId, sisSections);
    boolean success = boomiApi.updateWaitlistsForSections(sectionsList);
    if (!success) {
      log.error("Received error when sending waitlists to Boomi, waitlists were not enabled");
    }
  }

  public void removeWaitlistSections(String computingId, List<SisSection> sisSections) {
    List<CanvasWaitlist> sectionsList = new ArrayList<>();
    for (SisSection sisSection : sisSections) {
      CanvasWaitlist section =
          SectionUtils.sectionIdToCanvasWaitlist(sisSection.getSisSectionId(), false);
      if (section != null) {
        sectionsList.add(section);
      }
    }
    log.info(
        "User '{}' is disabling the following sections for waitlists: {}",
        computingId,
        sisSections);
    boolean success = boomiApi.updateWaitlistsForSections(sectionsList);
    if (!success) {
      log.error("Received error when sending waitlists to Boomi, waitlists were not disabled");
    }
  }

  public List<SisSection> getWaitlistStatusForSections(List<CanvasSection> canvasSections) {
    List<CanvasWaitlistStatus> sectionsStatusList = new ArrayList<>();
    for (CanvasSection canvasSection : canvasSections) {
      CanvasWaitlistStatus sectionStatus =
          SectionUtils.sectionIdToCanvasWaitlistStatus(canvasSection.sisSectionId());
      if (sectionStatus != null) {
        sectionsStatusList.add(sectionStatus);
      }
    }
    List<SisSection> sectionsWithWaitlistStatus =
        boomiApi.getWaitlistStatusForSections(sectionsStatusList);
    if (sectionsWithWaitlistStatus.isEmpty()) {
      log.error(
          "Received empty/bad response when attempting to get waitlist status for the following sections: {}",
          canvasSections);
    }
    return sectionsWithWaitlistStatus;
  }
}

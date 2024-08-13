package edu.virginia.its.canvas.section.service;

import edu.virginia.its.canvas.section.api.BoomiApi;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlist;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlistStatus;
import edu.virginia.its.canvas.section.model.BoomiResponses.SisSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.SectionDTO;
import edu.virginia.its.canvas.section.utils.SectionUtils;
import edu.virginia.its.canvas.section.utils.TermUtils;
import java.time.LocalDate;
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

  public void addWaitlistSections(String computingId, List<SectionDTO> sectionDTOS) {
    List<CanvasWaitlist> sectionsList = new ArrayList<>();
    for (SectionDTO sectionDTO : sectionDTOS) {
      CanvasWaitlist section = SectionUtils.sectionIdToCanvasWaitlist(sectionDTO.getSisId(), true);
      if (section != null) {
        sectionsList.add(section);
      }
    }
    log.info(
        "User '{}' is enabling the following sections for waitlists: {}", computingId, sectionDTOS);
    boomiApi.updateWaitlistsForSections(sectionsList);
  }

  public void removeWaitlistSections(String computingId, List<SectionDTO> sectionDTOS) {
    List<CanvasWaitlist> sectionsList = new ArrayList<>();
    for (SectionDTO sectionDTO : sectionDTOS) {
      CanvasWaitlist section = SectionUtils.sectionIdToCanvasWaitlist(sectionDTO.getSisId(), false);
      if (section != null) {
        sectionsList.add(section);
      }
    }
    log.info(
        "User '{}' is disabling the following sections for waitlists: {}",
        computingId,
        sectionDTOS);
    boomiApi.updateWaitlistsForSections(sectionsList);
  }

  public List<SisSection> getWaitlistStatusForSections(List<CanvasSection> canvasSections) {
    List<CanvasWaitlistStatus> sectionsStatusList = new ArrayList<>();
    String currentTerm = TermUtils.getCurrentTerm(LocalDate.now());
    for (CanvasSection canvasSection : canvasSections) {
      CanvasWaitlistStatus sectionStatus =
          SectionUtils.sectionIdToCanvasWaitlistStatus(canvasSection.sisSectionId());
      // We check the term here so we don't send to Boomi request for waitlist information on
      // sections from the past.
      if (sectionStatus != null && currentTerm.compareTo(sectionStatus.term()) <= 0) {
        sectionsStatusList.add(sectionStatus);
      }
    }
    List<SisSection> sectionsWithWaitlistStatus =
        boomiApi.getWaitlistStatusForSections(sectionsStatusList);
    List<SisSection> sisSections = new ArrayList<>(sectionsWithWaitlistStatus);
    if (sisSections.isEmpty()) {
      log.warn(
          "Received empty/bad response when attempting to get waitlist status for the following sections: {}",
          canvasSections);
    }
    return sisSections;
  }
}

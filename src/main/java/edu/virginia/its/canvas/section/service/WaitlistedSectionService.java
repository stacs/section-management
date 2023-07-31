package edu.virginia.its.canvas.section.service;

import edu.virginia.its.canvas.section.api.BoomiApi;
import edu.virginia.its.canvas.section.model.CanvasResponses.Section;
import edu.virginia.its.canvas.section.model.WaitlistedSection;
import edu.virginia.its.canvas.section.repos.WaitlistedSectionRepo;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WaitlistedSectionService {

  @Autowired private WaitlistedSectionRepo waitlistedSectionRepo;

  @Autowired private BoomiApi boomiApi;

  public void addWaitlistSections(String computingId, List<Section> sectionsToAddWaitlistsFor) {
    List<WaitlistedSection> waitlistedSectionList = new ArrayList<>();
    for (Section section : sectionsToAddWaitlistsFor) {
      WaitlistedSection waitlistedSection = convert(section, true);
      waitlistedSectionList.add(waitlistedSection);
    }
    log.info(
        "User '{}' is enabling the following sections for waitlists: {}",
        computingId,
        sectionsToAddWaitlistsFor);
    boolean success = boomiApi.updateWaitlistsForSections(waitlistedSectionList);
    if (success) {
      saveAll(waitlistedSectionList);
    } else {
      log.error("Received error when sending waitlists to Boomi, will not save entities to our DB");
    }
  }

  public void removeWaitlistSections(
      String computingId, List<Section> sectionsToRemoveWaitlistsFor) {
    List<WaitlistedSection> waitlistedSectionList = new ArrayList<>();
    for (Section section : sectionsToRemoveWaitlistsFor) {
      WaitlistedSection waitlistedSection = convert(section, false);
      waitlistedSectionList.add(waitlistedSection);
    }
    log.info(
        "User '{}' is disabling the following sections for waitlists: {}",
        computingId,
        sectionsToRemoveWaitlistsFor);
    boolean success = boomiApi.updateWaitlistsForSections(waitlistedSectionList);
    if (success) {
      saveAll(waitlistedSectionList);
    } else {
      log.error("Received error when sending waitlists to Boomi, will not save entities to our DB");
    }
  }

  public List<WaitlistedSection> findAllSections(List<Section> sections) {
    List<String> sectionSisIds = sections.stream().map(Section::sisSectionId).toList();
    return waitlistedSectionRepo.findBySisSectionIdIn(sectionSisIds);
  }

  public WaitlistedSection convert(Section section, boolean waitlisted) {
    WaitlistedSection waitlistedSection = new WaitlistedSection();
    waitlistedSection.setCanvasId(Long.valueOf(section.id()));
    waitlistedSection.setSisSectionId(section.sisSectionId());
    waitlistedSection.setWaitlisted(waitlisted);
    return waitlistedSection;
  }

  public void saveAll(List<WaitlistedSection> waitlistedSections) {
    waitlistedSectionRepo.saveAll(waitlistedSections);
  }
}

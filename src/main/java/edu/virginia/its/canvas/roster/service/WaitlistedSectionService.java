package edu.virginia.its.canvas.roster.service;

import edu.virginia.its.canvas.roster.model.CanvasResponses.Section;
import edu.virginia.its.canvas.roster.model.WaitlistedSection;
import edu.virginia.its.canvas.roster.repos.WaitlistedSectionRepo;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WaitlistedSectionService {

  @Autowired private WaitlistedSectionRepo waitlistedSectionRepo;

  public List<WaitlistedSection> findAllSections(List<Section> sections) {
    List<String> sectionSisIds = sections.stream().map(Section::sisSectionId).toList();
    return waitlistedSectionRepo.findBySisSectionIdIn(sectionSisIds);
  }

  public WaitlistedSection create(Section section) {
    WaitlistedSection waitlistedSection = new WaitlistedSection();
    waitlistedSection.setCanvasId(Long.valueOf(section.id()));
    waitlistedSection.setSisSectionId(section.sisSectionId());
    waitlistedSection.setWaitlisted(true);
    waitlistedSectionRepo.save(waitlistedSection);
    return waitlistedSection;
  }

  public WaitlistedSection update(WaitlistedSection waitlistedSection) {
    waitlistedSectionRepo.save(waitlistedSection);
    return waitlistedSection;
  }

  public void delete(WaitlistedSection waitlistedSection) {
    waitlistedSectionRepo.delete(waitlistedSection);
  }
}

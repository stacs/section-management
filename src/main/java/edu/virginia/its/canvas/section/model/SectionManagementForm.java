package edu.virginia.its.canvas.section.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class SectionManagementForm {

  private String selectedTerm;
  private List<String> sectionsAlreadyAdded = new ArrayList<>();
  private List<String> waitlistsAlreadyAdded = new ArrayList<>();

  private List<String> sectionsToKeep = new ArrayList<>();
  private List<String> sectionsToAdd = new ArrayList<>();
  private List<String> waitlistsToAdd = new ArrayList<>();
}

package edu.virginia.its.canvas.roster.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RosterManagementForm {

  private String selectedTerm;
  private List<String> sectionsToKeep = new ArrayList<>();
  private List<String> sectionsToAdd = new ArrayList<>();
}

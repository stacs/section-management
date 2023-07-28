package edu.virginia.its.canvas.roster.controller;

import edu.virginia.its.canvas.lti.util.CanvasAuthenticationToken;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Course;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Section;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Term;
import edu.virginia.its.canvas.roster.model.RosterManagementForm;
import edu.virginia.its.canvas.roster.model.WaitlistedSection;
import edu.virginia.its.canvas.roster.service.RosterManagementService;
import edu.virginia.its.canvas.roster.service.WaitlistedSectionService;
import edu.virginia.its.canvas.roster.utils.Constants;
import edu.virginia.its.canvas.roster.utils.SectionUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
public class RosterManagementController {

  @Autowired private RosterManagementService rosterManagementService;

  @Autowired private WaitlistedSectionService waitlistedSectionService;

  @GetMapping("/launch")
  public String launch() {
    // We don't need the token here, but we still call getToken() to ensure the LTI handshake was
    // successful.
    CanvasAuthenticationToken.getToken();
    return "loading";
  }

  @GetMapping("/index")
  public String index(Model model) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);

    Course currentCourse = rosterManagementService.getCourse(courseId);
    model.addAttribute("courseName", currentCourse.name());
    model.addAttribute("courseCode", currentCourse.courseCode());
    List<Section> currentCourseSections = rosterManagementService.getValidCourseSections(courseId);
    model.addAttribute("currentCourseSections", currentCourseSections);

    List<Course> userCourses = rosterManagementService.getUserCourses(computingId);
    Map<Term, List<Section>> sectionsMap =
        rosterManagementService.getAllUserSectionsGroupedByTerm(userCourses);
    // Remove any sections from the Map that are already in the course as sectionsMap is used to
    // show options to the user on what sections they can add to their course.
    // TODO: maybe move this logic into RosterManagementService?
    sectionsMap.values().forEach(sections -> sections.removeAll(currentCourseSections));
    sectionsMap.values().removeIf(List::isEmpty);
    model.addAttribute("sectionsMap", sectionsMap);

    // We need to pre-populate the form with the sections already added so the checkboxes for those
    // sections will be checked
    List<String> sectionsAlreadyAdded =
        currentCourseSections.stream().map(Section::id).collect(Collectors.toList());
    RosterManagementForm rosterManagementForm = new RosterManagementForm();
    rosterManagementForm.setSectionsToKeep(sectionsAlreadyAdded);
    model.addAttribute("rosterManagementForm", rosterManagementForm);

    return "index";
  }

  @PostMapping("/waitlists")
  public String waitlists(Model model, @ModelAttribute RosterManagementForm rosterManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<Course> userCourses = rosterManagementService.getUserCourses(computingId);
    List<Section> allSections = rosterManagementService.getAllUserSections(userCourses);
    List<Section> currentCourseSections = rosterManagementService.getValidCourseSections(courseId);
    List<Section> potentialWaitlistSections =
        getPotentialWaitlistSections(allSections, currentCourseSections, rosterManagementForm);
    SectionUtils.sortSectionsByName(potentialWaitlistSections);
    if (potentialWaitlistSections.isEmpty()) {
      return validate(model, rosterManagementForm);
    }
    model.addAttribute("potentialWaitlistSections", potentialWaitlistSections);

    // TODO: move query of waitlisted sections from our DB into Boomi?
    List<WaitlistedSection> waitlistedSectionList =
        waitlistedSectionService.findAllSections(potentialWaitlistSections);
    List<String> waitlistedSectionsAlreadyEnabled =
        waitlistedSectionList.stream()
            .filter(WaitlistedSection::isWaitlisted)
            .map(w -> Long.toString(w.getCanvasId()))
            .toList();
    rosterManagementForm.setWaitlistsToAdd(waitlistedSectionsAlreadyEnabled);
    return "waitlists";
  }

  @PostMapping("/validate")
  public String validate(Model model, @ModelAttribute RosterManagementForm rosterManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<Section> currentCourseSections = rosterManagementService.getValidCourseSections(courseId);
    // Don't try to remove sections that have a null crosslisted course id (meaning that they were
    // originally created in the current course).
    List<Section> sectionsToRemove =
        getSectionsToRemove(currentCourseSections, rosterManagementForm);
    model.addAttribute("sectionsToRemove", sectionsToRemove);

    List<Course> userCourses = rosterManagementService.getUserCourses(computingId);
    List<Section> allSections = rosterManagementService.getAllUserSections(userCourses);
    List<Section> sectionsToAdd = getSectionsToAdd(allSections, rosterManagementForm);
    model.addAttribute("sectionsToAdd", sectionsToAdd);

    List<Course> coursesToRemoveUserFrom =
        getCoursesToRemoveUserFrom(userCourses, allSections, rosterManagementForm);
    model.addAttribute("coursesToRemoveUserFrom", coursesToRemoveUserFrom);

    List<Section> potentialWaitlistSections =
        getPotentialWaitlistSections(allSections, currentCourseSections, rosterManagementForm);
    List<Section> waitlistedSectionsToAdd =
        potentialWaitlistSections.stream()
            .filter(section -> rosterManagementForm.getWaitlistsToAdd().contains(section.id()))
            .toList();
    model.addAttribute("waitlistedSectionsToAdd", waitlistedSectionsToAdd);
    List<Section> waitlistedSectionsToRemove =
        potentialWaitlistSections.stream()
            .filter(section -> !rosterManagementForm.getWaitlistsToAdd().contains(section.id()))
            .toList();
    model.addAttribute("waitlistedSectionsToRemove", waitlistedSectionsToRemove);
    return "validate";
  }

  @PostMapping("/apply-changes")
  public String applyChanges(
      Model model, @ModelAttribute RosterManagementForm rosterManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<Section> currentCourseSections = rosterManagementService.getValidCourseSections(courseId);
    // Don't try to remove sections that have a null crosslisted course id (meaning that they were
    // originally created in the current course).
    List<Section> sectionsToRemove =
        getSectionsToRemove(currentCourseSections, rosterManagementForm);
    List<Section> sectionsToRemoveErrors = new ArrayList<>();
    for (Section sectionToRemove : sectionsToRemove) {
      log.info(
          "User '{}' is decrosslisting section '{}' back to course '{}'",
          computingId,
          sectionToRemove,
          sectionToRemove.crosslistedCourseId());
      boolean success = rosterManagementService.deCrosslistSection(sectionToRemove);
      if (!success) {
        sectionsToRemoveErrors.add(sectionToRemove);
      }
    }
    model.addAttribute("sectionsToRemove", sectionsToRemove);
    model.addAttribute("sectionsToRemoveErrors", sectionsToRemoveErrors);

    List<Course> userCourses = rosterManagementService.getUserCourses(computingId);
    List<Section> allSections = rosterManagementService.getAllUserSections(userCourses);
    List<Section> sectionsToAdd = getSectionsToAdd(allSections, rosterManagementForm);
    List<Section> sectionsToAddErrors = new ArrayList<>();
    for (Section sectionToAdd : sectionsToAdd) {
      log.info(
          "User '{}' is crosslisting section '{}' to course '{}'",
          computingId,
          sectionToAdd,
          courseId);
      boolean success = rosterManagementService.crosslistSection(sectionToAdd, courseId);
      if (!success) {
        sectionsToAddErrors.add(sectionToAdd);
      }
    }
    model.addAttribute("sectionsToAdd", sectionsToAdd);
    model.addAttribute("sectionsToAddErrors", sectionsToAddErrors);

    if (rosterManagementForm.isRemoveFromCourse()) {
      List<Course> coursesToRemoveUserFrom =
          getCoursesToRemoveUserFrom(userCourses, allSections, rosterManagementForm);
      for (Course course : coursesToRemoveUserFrom) {
        log.info("Removing user '{}' from course '{}'", computingId, course.id());
        rosterManagementService.removeUserFromCourse(computingId, course.id());
      }
      model.addAttribute("coursesToRemoveUserFrom", coursesToRemoveUserFrom);
    }

    List<Section> potentialWaitlistSections =
        getPotentialWaitlistSections(allSections, currentCourseSections, rosterManagementForm);
    List<Section> waitlistedSectionsToAdd =
        potentialWaitlistSections.stream()
            .filter(section -> rosterManagementForm.getWaitlistsToAdd().contains(section.id()))
            .toList();
    if (!waitlistedSectionsToAdd.isEmpty()) {
      waitlistedSectionService.addWaitlistSections(computingId, waitlistedSectionsToAdd);
      model.addAttribute("waitlistedSectionsToAdd", waitlistedSectionsToAdd);
    }
    List<Section> waitlistedSectionsToRemove =
        potentialWaitlistSections.stream()
            .filter(section -> !rosterManagementForm.getWaitlistsToAdd().contains(section.id()))
            .toList();
    if (!waitlistedSectionsToRemove.isEmpty()) {
      waitlistedSectionService.removeWaitlistSections(computingId, waitlistedSectionsToRemove);
      model.addAttribute("waitlistedSectionsToRemove", waitlistedSectionsToRemove);
    }

    return "success";
  }

  private Set<Section> getSectionsBeingAddedOrKept(
      List<Section> allSections, RosterManagementForm rosterManagementForm) {
    return new HashSet<>(
        allSections.stream()
            .filter(
                section ->
                    rosterManagementForm.getSectionsToAdd().contains(section.id())
                        || rosterManagementForm.getSectionsToKeep().contains(section.id()))
            .toList());
  }

  private List<Section> getSectionsToRemove(
      List<Section> currentCourseSections, RosterManagementForm rosterManagementForm) {
    return currentCourseSections.stream()
        .filter(
            section ->
                section.crosslistedCourseId() != null
                    && !rosterManagementForm.getSectionsToKeep().contains(section.id()))
        .toList();
  }

  private List<Section> getSectionsToAdd(
      List<Section> allSections, RosterManagementForm rosterManagementForm) {
    return allSections.stream()
        .filter(section -> rosterManagementForm.getSectionsToAdd().contains(section.id()))
        .toList();
  }

  private List<Course> getCoursesToRemoveUserFrom(
      List<Course> userCourses,
      List<Section> allSections,
      RosterManagementForm rosterManagementForm) {
    List<Section> sectionsToAdd = getSectionsToAdd(allSections, rosterManagementForm);
    return userCourses.stream()
        .filter(
            course ->
                sectionsToAdd.stream().anyMatch(section -> section.courseId().equals(course.id())))
        .toList();
  }

  private List<Section> getPotentialWaitlistSections(
      List<Section> allSections,
      List<Section> currentCourseSections,
      RosterManagementForm rosterManagementForm) {
    Set<Section> sectionsToCheckSet =
        getSectionsBeingAddedOrKept(allSections, rosterManagementForm);

    // Have to manually add the current course sections as we don't pass that in the
    // SectionsToKeep object as we don't let the user remove the original SIS sections from a
    // course.
    // The reason SectionsToCheck starts as a Set is so we don't double show Sections that were
    // already in the course and are being kept in.
    sectionsToCheckSet.addAll(currentCourseSections);

    // Don't show sections that we are in the process of removing
    List<Section> sectionsToRemove =
        getSectionsToRemove(currentCourseSections, rosterManagementForm);
    sectionsToRemove.forEach(sectionsToCheckSet::remove);

    return new ArrayList<>(sectionsToCheckSet);
  }
}

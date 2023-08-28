package edu.virginia.its.canvas.section.controller;

import edu.virginia.its.canvas.lti.util.CanvasAuthenticationToken;
import edu.virginia.its.canvas.lti.util.Constants;
import edu.virginia.its.canvas.section.model.CanvasResponses.Course;
import edu.virginia.its.canvas.section.model.CanvasResponses.Section;
import edu.virginia.its.canvas.section.model.CanvasResponses.Term;
import edu.virginia.its.canvas.section.model.SectionManagementForm;
import edu.virginia.its.canvas.section.model.WaitlistedSection;
import edu.virginia.its.canvas.section.service.SectionManagementService;
import edu.virginia.its.canvas.section.service.WaitlistedSectionService;
import edu.virginia.its.canvas.section.utils.SectionUtils;
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
public class SectionManagementController {

  @Autowired private SectionManagementService sectionManagementService;

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

    Course currentCourse = sectionManagementService.getCourse(courseId);
    model.addAttribute("courseName", currentCourse.name());
    model.addAttribute("courseCode", currentCourse.courseCode());
    List<Section> currentCourseSections = sectionManagementService.getValidCourseSections(courseId);
    model.addAttribute("currentCourseSections", currentCourseSections);

    List<Course> userCourses = sectionManagementService.getUserCourses(computingId);
    Map<Term, List<Section>> sectionsMap =
        sectionManagementService.getAllUserSectionsGroupedByTerm(userCourses);
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
    SectionManagementForm sectionManagementForm = new SectionManagementForm();
    sectionManagementForm.setSectionsToKeep(sectionsAlreadyAdded);
    model.addAttribute("sectionManagementForm", sectionManagementForm);

    return "index";
  }

  @PostMapping("/waitlists")
  public String waitlists(
      Model model, @ModelAttribute SectionManagementForm sectionManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<Course> userCourses = sectionManagementService.getUserCourses(computingId);
    List<Section> allSections = sectionManagementService.getAllUserSections(userCourses);
    List<Section> currentCourseSections = sectionManagementService.getValidCourseSections(courseId);
    List<Section> potentialWaitlistSections =
        getPotentialWaitlistSections(allSections, currentCourseSections, sectionManagementForm);
    SectionUtils.sortSectionsByName(potentialWaitlistSections);
    if (potentialWaitlistSections.isEmpty()) {
      return validate(model, sectionManagementForm);
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
    sectionManagementForm.setWaitlistsToAdd(waitlistedSectionsAlreadyEnabled);
    return "waitlists";
  }

  @PostMapping("/validate")
  public String validate(Model model, @ModelAttribute SectionManagementForm sectionManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<Section> currentCourseSections = sectionManagementService.getValidCourseSections(courseId);
    // Don't try to remove sections that have a null crosslisted course id (meaning that they were
    // originally created in the current course).
    List<Section> sectionsToRemove =
        getSectionsToRemove(currentCourseSections, sectionManagementForm);
    model.addAttribute("sectionsToRemove", sectionsToRemove);

    List<Course> userCourses = sectionManagementService.getUserCourses(computingId);
    List<Section> allSections = sectionManagementService.getAllUserSections(userCourses);
    List<Section> sectionsToAdd = getSectionsToAdd(allSections, sectionManagementForm);
    model.addAttribute("sectionsToAdd", sectionsToAdd);

    List<Course> coursesToRemoveUserFrom =
        getCoursesToRemoveUserFrom(userCourses, allSections, sectionManagementForm);
    model.addAttribute("coursesToRemoveUserFrom", coursesToRemoveUserFrom);

    List<Section> potentialWaitlistSections =
        getPotentialWaitlistSections(allSections, currentCourseSections, sectionManagementForm);
    List<WaitlistedSection> waitlistedSectionList =
        waitlistedSectionService.findAllSections(potentialWaitlistSections);
    List<Section> waitlistedSectionsToAdd =
        getWaitlistSectionsToAdd(
            potentialWaitlistSections, waitlistedSectionList, sectionManagementForm);
    model.addAttribute("waitlistedSectionsToAdd", waitlistedSectionsToAdd);
    List<Section> waitlistedSectionsToRemove =
        getWaitlistSectionsToRemove(
            potentialWaitlistSections, waitlistedSectionList, sectionManagementForm);
    model.addAttribute("waitlistedSectionsToRemove", waitlistedSectionsToRemove);
    return "validate";
  }

  @PostMapping("/apply-changes")
  public String applyChanges(
      Model model, @ModelAttribute SectionManagementForm sectionManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<Section> currentCourseSections = sectionManagementService.getValidCourseSections(courseId);
    // Don't try to remove sections that have a null crosslisted course id (meaning that they were
    // originally created in the current course).
    List<Section> sectionsToRemove =
        getSectionsToRemove(currentCourseSections, sectionManagementForm);
    List<Section> sectionsToRemoveErrors = new ArrayList<>();
    for (Section sectionToRemove : sectionsToRemove) {
      log.info(
          "User '{}' is decrosslisting section '{}' back to course '{}'",
          computingId,
          sectionToRemove,
          sectionToRemove.crosslistedCourseId());
      boolean success = sectionManagementService.deCrosslistSection(sectionToRemove);
      if (!success) {
        sectionsToRemoveErrors.add(sectionToRemove);
      }
    }
    model.addAttribute("sectionsToRemove", sectionsToRemove);
    model.addAttribute("sectionsToRemoveErrors", sectionsToRemoveErrors);

    List<Course> userCourses = sectionManagementService.getUserCourses(computingId);
    List<Section> allSections = sectionManagementService.getAllUserSections(userCourses);
    List<Section> sectionsToAdd = getSectionsToAdd(allSections, sectionManagementForm);
    List<Section> sectionsToAddErrors = new ArrayList<>();
    for (Section sectionToAdd : sectionsToAdd) {
      log.info(
          "User '{}' is crosslisting section '{}' to course '{}'",
          computingId,
          sectionToAdd,
          courseId);
      boolean success = sectionManagementService.crosslistSection(sectionToAdd, courseId);
      if (!success) {
        sectionsToAddErrors.add(sectionToAdd);
      }
    }
    model.addAttribute("sectionsToAdd", sectionsToAdd);
    model.addAttribute("sectionsToAddErrors", sectionsToAddErrors);

    List<Section> potentialWaitlistSections =
        getPotentialWaitlistSections(allSections, currentCourseSections, sectionManagementForm);
    List<WaitlistedSection> waitlistedSectionList =
        waitlistedSectionService.findAllSections(potentialWaitlistSections);
    List<Section> waitlistedSectionsToAdd =
        getWaitlistSectionsToAdd(
            potentialWaitlistSections, waitlistedSectionList, sectionManagementForm);
    if (!waitlistedSectionsToAdd.isEmpty()) {
      waitlistedSectionService.addWaitlistSections(computingId, waitlistedSectionsToAdd);
      model.addAttribute("waitlistedSectionsToAdd", waitlistedSectionsToAdd);
    }
    List<Section> waitlistedSectionsToRemove =
        getWaitlistSectionsToRemove(
            potentialWaitlistSections, waitlistedSectionList, sectionManagementForm);
    if (!waitlistedSectionsToRemove.isEmpty()) {
      waitlistedSectionService.removeWaitlistSections(computingId, waitlistedSectionsToRemove);
      model.addAttribute("waitlistedSectionsToRemove", waitlistedSectionsToRemove);
    }

    return "success";
  }

  private Set<Section> getSectionsBeingAddedOrKept(
      List<Section> allSections, SectionManagementForm sectionManagementForm) {
    return new HashSet<>(
        allSections.stream()
            .filter(
                section ->
                    sectionManagementForm.getSectionsToAdd().contains(section.id())
                        || sectionManagementForm.getSectionsToKeep().contains(section.id()))
            .toList());
  }

  private List<Section> getSectionsToRemove(
      List<Section> currentCourseSections, SectionManagementForm sectionManagementForm) {
    return currentCourseSections.stream()
        .filter(
            section ->
                section.crosslistedCourseId() != null
                    && !sectionManagementForm.getSectionsToKeep().contains(section.id()))
        .toList();
  }

  private List<Section> getSectionsToAdd(
      List<Section> allSections, SectionManagementForm sectionManagementForm) {
    return allSections.stream()
        .filter(section -> sectionManagementForm.getSectionsToAdd().contains(section.id()))
        .toList();
  }

  private List<Course> getCoursesToRemoveUserFrom(
      List<Course> userCourses,
      List<Section> allSections,
      SectionManagementForm sectionManagementForm) {
    List<Section> sectionsToAdd = getSectionsToAdd(allSections, sectionManagementForm);
    return userCourses.stream()
        .filter(
            course ->
                sectionsToAdd.stream().anyMatch(section -> section.courseId().equals(course.id())))
        .toList();
  }

  private List<Section> getPotentialWaitlistSections(
      List<Section> allSections,
      List<Section> currentCourseSections,
      SectionManagementForm sectionManagementForm) {
    Set<Section> sectionsToCheckSet =
        getSectionsBeingAddedOrKept(allSections, sectionManagementForm);

    // Have to manually add the current course sections as we don't pass that in the
    // SectionsToKeep object as we don't let the user remove the original SIS sections from a
    // course.
    // The reason SectionsToCheck starts as a Set is so we don't double show Sections that were
    // already in the course and are being kept in.
    sectionsToCheckSet.addAll(currentCourseSections);

    // Don't show sections that we are in the process of removing
    List<Section> sectionsToRemove =
        getSectionsToRemove(currentCourseSections, sectionManagementForm);
    sectionsToRemove.forEach(sectionsToCheckSet::remove);

    return new ArrayList<>(sectionsToCheckSet);
  }

  private List<Section> getWaitlistSectionsToAdd(
      List<Section> potentialWaitlistSections,
      List<WaitlistedSection> waitlistedSectionList,
      SectionManagementForm sectionManagementForm) {
    // We only want to return the Sections that are actually changing, so we compare against the
    // WaitlistedSection list from the DB to see if any changes were actually made.
    List<Section> waitlistSectionsToAdd = new ArrayList<>();
    List<Section> potentialWaitlistSectionsToAdd =
        potentialWaitlistSections.stream()
            .filter(section -> sectionManagementForm.getWaitlistsToAdd().contains(section.id()))
            .toList();
    // Couldn't figure out a way to do this via a single stream call since a null DB row (no
    // matching WaitlistSection) means waitlists are
    // turned off for that section, but the DB could also have a row for a section that has no
    // waitlists (when setting a waitlist from enabled
    // to disabled we set the waitlist flag for the row to false vs removing the row).
    // TODO: Maybe remove rows when turning off waitlists?  Will be a moot point if/when we ever
    // move the read request for waitlist sections to Boomi.
    for (Section section : potentialWaitlistSectionsToAdd) {
      WaitlistedSection waitlistedSection =
          waitlistedSectionList.stream()
              .filter(waitlist -> waitlist.getSisSectionId().equals(section.sisSectionId()))
              .findFirst()
              .orElse(null);
      if (waitlistedSection == null) {
        waitlistSectionsToAdd.add(section);
      } else {
        if (!waitlistedSection.isWaitlisted()) {
          waitlistSectionsToAdd.add(section);
        }
      }
    }
    return waitlistSectionsToAdd;
  }

  private List<Section> getWaitlistSectionsToRemove(
      List<Section> potentialWaitlistSections,
      List<WaitlistedSection> waitlistedSectionList,
      SectionManagementForm sectionManagementForm) {
    // We only want to return the Sections that are actually changing, so we compare against the
    // WaitlistedSection list from the DB to see if any changes were actually made.
    // This version is pretty simple compared to the version for added waitlists because we know we
    // will always have a DB row.
    return potentialWaitlistSections.stream()
        .filter(
            section ->
                !sectionManagementForm.getWaitlistsToAdd().contains(section.id())
                    && waitlistedSectionList.stream()
                        .anyMatch(
                            waitlist ->
                                waitlist.getSisSectionId().equals(section.sisSectionId())
                                    && waitlist.isWaitlisted()))
        .toList();
  }
}

package edu.virginia.its.canvas.section.controller;

import edu.virginia.its.canvas.lti.util.CanvasAuthenticationToken;
import edu.virginia.its.canvas.lti.util.Constants;
import edu.virginia.its.canvas.section.model.BoomiResponses.SisSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.Course;
import edu.virginia.its.canvas.section.model.CanvasResponses.Term;
import edu.virginia.its.canvas.section.model.SectionManagementForm;
import edu.virginia.its.canvas.section.service.SectionManagementService;
import edu.virginia.its.canvas.section.service.WaitlistedSectionService;
import edu.virginia.its.canvas.section.utils.SectionUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
public class SectionManagementController {

  private final SectionManagementService sectionManagementService;

  private final WaitlistedSectionService waitlistedSectionService;

  public SectionManagementController(
      SectionManagementService sectionManagementService,
      WaitlistedSectionService waitlistedSectionService) {
    this.sectionManagementService = sectionManagementService;
    this.waitlistedSectionService = waitlistedSectionService;
  }

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
    List<CanvasSection> currentCourseCanvasSections =
        sectionManagementService.getValidCourseSections(courseId);
    // Show un-removable sections first in the UI, then show the list of sections that can be
    // removed sorting both groups by name.
    currentCourseCanvasSections.sort(SectionUtils.ALREADY_ADDED_SECTIONS_COMPARATOR);
    model.addAttribute("currentCourseSections", currentCourseCanvasSections);

    // TODO: Should the currentCourseSections attribute be a list of SisSections vs CanvasSections
    // so we only have one data structure to manage?
    // I'm using CanvasSection for now so I could still show this page even if Boomi/SisSections are
    // down.
    List<SisSection> sectionsWithWaitlistStatus =
        waitlistedSectionService.getWaitlistStatusForSections(currentCourseCanvasSections);
    Map<String, SisSection> waitlistSectionsMap =
        sectionsWithWaitlistStatus.stream()
            .filter(SisSection::hasWaitlist)
            .collect(Collectors.toMap(SisSection::getSisSectionId, sisSection -> sisSection));
    model.addAttribute("waitlistSectionsMap", waitlistSectionsMap);

    List<Course> userCourses = sectionManagementService.getUserCourses(computingId);
    Map<Term, List<CanvasSection>> sectionsMap =
        sectionManagementService.getAllUserSectionsGroupedByTerm(userCourses);
    // Remove any sections from the Map that are already in the course as sectionsMap is used to
    // show options to the user on what sections they can add to their course.
    // TODO: maybe move this logic into RosterManagementService?
    sectionsMap.values().forEach(sections -> sections.removeAll(currentCourseCanvasSections));
    sectionsMap.values().removeIf(List::isEmpty);
    model.addAttribute("sectionsMap", sectionsMap);

    // We need to pre-populate the form with the sections already added so the checkboxes for those
    // sections will be checked
    List<String> sectionsAlreadyAdded =
        currentCourseCanvasSections.stream().map(CanvasSection::id).toList();
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
    List<CanvasSection> allCanvasSections =
        sectionManagementService.getAllUserSections(userCourses);
    List<CanvasSection> currentCourseCanvasSections =
        sectionManagementService.getValidCourseSections(courseId);
    List<CanvasSection> potentialWaitlistCanvasSections =
        getPotentialWaitlistSections(
            allCanvasSections, currentCourseCanvasSections, sectionManagementForm);
    if (potentialWaitlistCanvasSections.isEmpty()) {
      return validate(model, sectionManagementForm);
    }

    List<SisSection> waitlistStatusForSections =
        waitlistedSectionService.getWaitlistStatusForSections(potentialWaitlistCanvasSections);
    model.addAttribute("waitlistStatusForSections", waitlistStatusForSections);
    List<String> waitlistedSectionsAlreadyEnabled =
        waitlistStatusForSections.stream()
            .filter(SisSection::hasWaitlist)
            .map(SisSection::getSisSectionId)
            .toList();
    sectionManagementForm.setWaitlistsToAdd(waitlistedSectionsAlreadyEnabled);
    // TODO: what should happen when Boomi isn't reachable?
    return "waitlists";
  }

  @PostMapping("/validate")
  public String validate(Model model, @ModelAttribute SectionManagementForm sectionManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<CanvasSection> currentCourseCanvasSections =
        sectionManagementService.getValidCourseSections(courseId);
    // Don't try to remove sections that have a null crosslisted course id (meaning that they were
    // originally created in the current course).
    List<CanvasSection> sectionsToRemove =
        getSectionsToRemove(currentCourseCanvasSections, sectionManagementForm);
    model.addAttribute("sectionsToRemove", sectionsToRemove);

    List<Course> userCourses = sectionManagementService.getUserCourses(computingId);
    List<CanvasSection> allCanvasSections =
        sectionManagementService.getAllUserSections(userCourses);
    List<CanvasSection> sectionsToAdd = getSectionsToAdd(allCanvasSections, sectionManagementForm);
    model.addAttribute("sectionsToAdd", sectionsToAdd);

    List<Course> coursesToRemoveUserFrom =
        getCoursesToRemoveUserFrom(userCourses, allCanvasSections, sectionManagementForm);
    model.addAttribute("coursesToRemoveUserFrom", coursesToRemoveUserFrom);

    List<CanvasSection> potentialWaitlistCanvasSections =
        getPotentialWaitlistSections(
            allCanvasSections, currentCourseCanvasSections, sectionManagementForm);
    List<SisSection> waitlistedSectionList =
        waitlistedSectionService.getWaitlistStatusForSections(potentialWaitlistCanvasSections);

    List<SisSection> waitlistedSectionsToAdd =
        getWaitlistSectionsToAdd(waitlistedSectionList, sectionManagementForm);
    model.addAttribute("waitlistedSectionsToAdd", waitlistedSectionsToAdd);

    List<SisSection> waitlistedSectionsToRemove =
        getWaitlistSectionsToRemove(waitlistedSectionList, sectionManagementForm);
    model.addAttribute("waitlistedSectionsToRemove", waitlistedSectionsToRemove);
    return "validate";
  }

  @PostMapping("/apply-changes")
  public String applyChanges(
      Model model, @ModelAttribute SectionManagementForm sectionManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<CanvasSection> currentCourseCanvasSections =
        sectionManagementService.getValidCourseSections(courseId);
    // Don't try to remove sections that have a null crosslisted course id (meaning that they were
    // originally created in the current course).
    List<CanvasSection> sectionsToRemove =
        getSectionsToRemove(currentCourseCanvasSections, sectionManagementForm);
    List<CanvasSection> sectionsToRemoveErrors = new ArrayList<>();
    for (CanvasSection canvasSectionToRemove : sectionsToRemove) {
      log.info(
          "User '{}' is decrosslisting section '{}' back to course '{}'",
          computingId,
          canvasSectionToRemove,
          canvasSectionToRemove.crosslistedCourseId());
      boolean success = sectionManagementService.deCrosslistSection(canvasSectionToRemove);
      if (!success) {
        sectionsToRemoveErrors.add(canvasSectionToRemove);
      }
    }
    model.addAttribute("sectionsToRemove", sectionsToRemove);
    model.addAttribute("sectionsToRemoveErrors", sectionsToRemoveErrors);

    List<Course> userCourses = sectionManagementService.getUserCourses(computingId);
    List<CanvasSection> allCanvasSections =
        sectionManagementService.getAllUserSections(userCourses);
    List<CanvasSection> sectionsToAdd = getSectionsToAdd(allCanvasSections, sectionManagementForm);
    List<CanvasSection> sectionsToAddErrors = new ArrayList<>();
    for (CanvasSection canvasSectionToAdd : sectionsToAdd) {
      log.info(
          "User '{}' is crosslisting section '{}' to course '{}'",
          computingId,
          canvasSectionToAdd,
          courseId);
      boolean success = sectionManagementService.crosslistSection(canvasSectionToAdd, courseId);
      if (!success) {
        sectionsToAddErrors.add(canvasSectionToAdd);
      }
    }
    model.addAttribute("sectionsToAdd", sectionsToAdd);
    model.addAttribute("sectionsToAddErrors", sectionsToAddErrors);

    List<CanvasSection> potentialWaitlistCanvasSections =
        getPotentialWaitlistSections(
            allCanvasSections, currentCourseCanvasSections, sectionManagementForm);
    List<SisSection> waitlistedSectionList =
        waitlistedSectionService.getWaitlistStatusForSections(potentialWaitlistCanvasSections);

    List<SisSection> waitlistedSectionsToAdd =
        getWaitlistSectionsToAdd(waitlistedSectionList, sectionManagementForm);
    if (!waitlistedSectionsToAdd.isEmpty()) {
      waitlistedSectionService.addWaitlistSections(computingId, waitlistedSectionsToAdd);
      model.addAttribute("waitlistedSectionsToAdd", waitlistedSectionsToAdd);
    }

    List<SisSection> waitlistedSectionsToRemove =
        getWaitlistSectionsToRemove(waitlistedSectionList, sectionManagementForm);
    if (!waitlistedSectionsToRemove.isEmpty()) {
      waitlistedSectionService.removeWaitlistSections(computingId, waitlistedSectionsToRemove);
      model.addAttribute("waitlistedSectionsToRemove", waitlistedSectionsToRemove);
    }

    return "success";
  }

  private Set<CanvasSection> getSectionsBeingAddedOrKept(
      List<CanvasSection> allCanvasSections, SectionManagementForm sectionManagementForm) {
    return new HashSet<>(
        allCanvasSections.stream()
            .filter(
                section ->
                    sectionManagementForm.getSectionsToAdd().contains(section.id())
                        || sectionManagementForm.getSectionsToKeep().contains(section.id()))
            .toList());
  }

  private List<CanvasSection> getSectionsToRemove(
      List<CanvasSection> currentCourseCanvasSections,
      SectionManagementForm sectionManagementForm) {
    return currentCourseCanvasSections.stream()
        .filter(
            section ->
                section.crosslistedCourseId() != null
                    && !sectionManagementForm.getSectionsToKeep().contains(section.id()))
        .toList();
  }

  private List<CanvasSection> getSectionsToAdd(
      List<CanvasSection> allCanvasSections, SectionManagementForm sectionManagementForm) {
    return allCanvasSections.stream()
        .sorted(SectionUtils.SECTION_NAME_COMPARATOR)
        .filter(section -> sectionManagementForm.getSectionsToAdd().contains(section.id()))
        .toList();
  }

  private List<Course> getCoursesToRemoveUserFrom(
      List<Course> userCourses,
      List<CanvasSection> allCanvasSections,
      SectionManagementForm sectionManagementForm) {
    List<CanvasSection> sectionsToAdd = getSectionsToAdd(allCanvasSections, sectionManagementForm);
    return userCourses.stream()
        .filter(
            course ->
                sectionsToAdd.stream().anyMatch(section -> section.courseId().equals(course.id())))
        .toList();
  }

  private List<CanvasSection> getPotentialWaitlistSections(
      List<CanvasSection> allCanvasSections,
      List<CanvasSection> currentCourseCanvasSections,
      SectionManagementForm sectionManagementForm) {
    Set<CanvasSection> sectionsToCheckSet =
        getSectionsBeingAddedOrKept(allCanvasSections, sectionManagementForm);

    // Have to manually add the current course sections as we don't pass that in the
    // SectionsToKeep object as we don't let the user remove the original SIS sections from a
    // course.
    // The reason SectionsToCheck starts as a Set is so we don't double show Sections that were
    // already in the course and are being kept in.
    sectionsToCheckSet.addAll(currentCourseCanvasSections);

    // Don't show sections that we are in the process of removing
    List<CanvasSection> sectionsToRemove =
        getSectionsToRemove(currentCourseCanvasSections, sectionManagementForm);
    sectionsToRemove.forEach(sectionsToCheckSet::remove);

    List<CanvasSection> potentialWaitlistCanvasSections = new ArrayList<>(sectionsToCheckSet);
    potentialWaitlistCanvasSections.sort(SectionUtils.SECTION_NAME_COMPARATOR);
    return potentialWaitlistCanvasSections;
  }

  private List<SisSection> getWaitlistSectionsToAdd(
      List<SisSection> sections, SectionManagementForm sectionManagementForm) {
    return sections.stream()
        .sorted(Comparator.comparing(SisSection::getSisSectionId))
        .filter(
            section ->
                !section.hasWaitlist()
                    && sectionManagementForm
                        .getWaitlistsToAdd()
                        .contains(section.getSisSectionId()))
        .toList();
  }

  private List<SisSection> getWaitlistSectionsToRemove(
      List<SisSection> sections, SectionManagementForm sectionManagementForm) {
    return sections.stream()
        .sorted(Comparator.comparing(SisSection::getSisSectionId))
        .filter(
            section ->
                section.hasWaitlist()
                    && !sectionManagementForm
                        .getWaitlistsToAdd()
                        .contains(section.getSisSectionId()))
        .toList();
  }
}

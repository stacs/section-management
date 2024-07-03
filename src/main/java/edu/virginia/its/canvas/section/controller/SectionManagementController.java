package edu.virginia.its.canvas.section.controller;

import static java.util.stream.Collectors.groupingBy;

import edu.virginia.its.canvas.lti.util.CanvasAuthenticationToken;
import edu.virginia.its.canvas.lti.util.Constants;
import edu.virginia.its.canvas.section.model.CanvasResponses.Course;
import edu.virginia.its.canvas.section.model.SectionDTO;
import edu.virginia.its.canvas.section.model.SectionManagementForm;
import edu.virginia.its.canvas.section.model.TermDTO;
import edu.virginia.its.canvas.section.service.SectionManagementService;
import edu.virginia.its.canvas.section.service.WaitlistedSectionService;
import edu.virginia.its.canvas.section.utils.SectionUtils;
import java.util.*;
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
    model.addAttribute("courseTermName", currentCourse.term().name());

    List<SectionDTO> usersTeachingSections =
        sectionManagementService.getUsersTeachingSections(computingId);
    List<SectionDTO> sectionsInCurrentCourse =
        usersTeachingSections.stream()
            .filter(sectionDTO -> courseId.equals(sectionDTO.getCourseId()))
            // Show un-removable sections first in the UI, then show the list of sections that
            // can be removed sorting both groups by name.
            .sorted(SectionUtils.ALREADY_ADDED_SECTIONS_COMPARATOR)
            .toList();
    List<SectionDTO> sectionsInOtherCourses =
        usersTeachingSections.stream()
            .filter(sectionDTO -> !courseId.equals(sectionDTO.getCourseId()))
            .toList();

    model.addAttribute("allSections", usersTeachingSections);
    model.addAttribute("currentCourseSections", sectionsInCurrentCourse);

    // TODO: make sure terms are sorted by sis term id in the template
    Map<TermDTO, List<SectionDTO>> sectionsMap =
        sectionsInOtherCourses.stream().collect(groupingBy(SectionDTO::getTerm));

    model.addAttribute("sectionsMap", sectionsMap);

    // We need to pre-populate the form with the sections and waitlists already added so the
    // checkboxes for those sections will be checked
    List<String> sectionIdsInCurrentCourse =
        sectionsInCurrentCourse.stream().map(SectionDTO::getId).toList();
    //    TODO: should we filter waitlists via waitlistDataFound boolean instead of via allSections?
    //            should we show waitlists that we couldnt find data for in their own section/div?
    List<String> waitlistedSectionsAlreadyEnabled =
        usersTeachingSections.stream()
            .filter(SectionDTO::isWaitlist)
            .map(SectionDTO::getSisId)
            .toList();
    SectionManagementForm sectionManagementForm = new SectionManagementForm();
    sectionManagementForm.setSectionsToKeep(sectionIdsInCurrentCourse);
    sectionManagementForm.setWaitlistsToAdd(waitlistedSectionsAlreadyEnabled);
    model.addAttribute("sectionManagementForm", sectionManagementForm);

    return "index";
  }

  @PostMapping("/apply-changes")
  public String applyChanges(
      Model model, @ModelAttribute SectionManagementForm sectionManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<SectionDTO> usersTeachingSections =
        sectionManagementService.getUsersTeachingSections(computingId);
    List<SectionDTO> sectionsInCurrentCourse =
        usersTeachingSections.stream()
            .filter(sectionDTO -> courseId.equals(sectionDTO.getCourseId()))
            // Show un-removable sections first in the UI, then show the list of sections that
            // can be removed sorting both groups by name.
            .sorted(SectionUtils.ALREADY_ADDED_SECTIONS_COMPARATOR)
            .toList();
    List<SectionDTO> sectionsInOtherCourses =
        usersTeachingSections.stream()
            .filter(sectionDTO -> !courseId.equals(sectionDTO.getCourseId()))
            .toList();
    // Don't try to remove sections that have a null crosslisted course id (meaning that they were
    // originally created in the current course).
    List<SectionDTO> sectionsToRemove =
        getSectionsToRemove(sectionsInCurrentCourse, sectionManagementForm);
    List<SectionDTO> sectionsToRemoveErrors = new ArrayList<>();
    for (SectionDTO canvasSectionToRemove : sectionsToRemove) {
      log.info(
          "User '{}' is decrosslisting section '{}' back to course '{}'",
          computingId,
          canvasSectionToRemove,
          canvasSectionToRemove.getCrosslistedCourseId());
      boolean success = sectionManagementService.deCrosslistSection(canvasSectionToRemove);
      if (!success) {
        sectionsToRemoveErrors.add(canvasSectionToRemove);
      }
    }
    model.addAttribute("sectionsToRemove", sectionsToRemove);
    model.addAttribute("sectionsToRemoveErrors", sectionsToRemoveErrors);

    List<SectionDTO> sectionsToAdd =
        getSectionsToAdd(sectionsInOtherCourses, sectionManagementForm);
    List<SectionDTO> sectionsToAddErrors = new ArrayList<>();
    for (SectionDTO canvasSectionToAdd : sectionsToAdd) {
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

    List<SectionDTO> waitlistedSectionsToAdd =
        getWaitlistSectionsToAdd(usersTeachingSections, sectionManagementForm);
    if (!waitlistedSectionsToAdd.isEmpty()) {
      waitlistedSectionService.addWaitlistSections(computingId, waitlistedSectionsToAdd);
      model.addAttribute("waitlistedSectionsToAdd", waitlistedSectionsToAdd);
    }

    List<SectionDTO> waitlistedSectionsToRemove =
        getWaitlistSectionsToRemove(usersTeachingSections, sectionManagementForm);
    if (!waitlistedSectionsToRemove.isEmpty()) {
      waitlistedSectionService.removeWaitlistSections(computingId, waitlistedSectionsToRemove);
      model.addAttribute("waitlistedSectionsToRemove", waitlistedSectionsToRemove);
    }

    return "success";
  }

  private List<SectionDTO> getSectionsToRemove(
      List<SectionDTO> currentCourseCanvasSections, SectionManagementForm sectionManagementForm) {
    return currentCourseCanvasSections.stream()
        .filter(
            section ->
                section.isCrosslisted()
                    && !sectionManagementForm.getSectionsToKeep().contains(section.getId()))
        .toList();
  }

  private List<SectionDTO> getSectionsToAdd(
      List<SectionDTO> allCanvasSections, SectionManagementForm sectionManagementForm) {
    return allCanvasSections.stream()
        .sorted(SectionUtils.SECTION_NAME_COMPARATOR)
        .filter(section -> sectionManagementForm.getSectionsToAdd().contains(section.getId()))
        .toList();
  }

  private List<Course> getCoursesToRemoveUserFrom(
      List<Course> userCourses,
      List<SectionDTO> allCanvasSections,
      SectionManagementForm sectionManagementForm) {
    List<SectionDTO> sectionsToAdd = getSectionsToAdd(allCanvasSections, sectionManagementForm);
    return userCourses.stream()
        .filter(
            course ->
                sectionsToAdd.stream()
                    .anyMatch(section -> section.getCourseId().equals(course.id())))
        .toList();
  }

  private List<SectionDTO> getWaitlistSectionsToAdd(
      List<SectionDTO> sections, SectionManagementForm sectionManagementForm) {
    return sections.stream()
        .sorted(Comparator.comparing(SectionDTO::getSisId))
        .filter(
            section ->
                !section.isWaitlist()
                    && sectionManagementForm.getWaitlistsToAdd().contains(section.getSisId()))
        .toList();
  }

  private List<SectionDTO> getWaitlistSectionsToRemove(
      List<SectionDTO> sections, SectionManagementForm sectionManagementForm) {
    return sections.stream()
        .sorted(Comparator.comparing(SectionDTO::getSisId))
        .filter(
            section ->
                section.isWaitlist()
                    && !sectionManagementForm.getWaitlistsToAdd().contains(section.getSisId()))
        .toList();
  }
}

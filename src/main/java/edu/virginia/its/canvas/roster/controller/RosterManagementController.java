package edu.virginia.its.canvas.roster.controller;

import edu.virginia.its.canvas.lti.util.CanvasAuthenticationToken;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Course;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Section;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Term;
import edu.virginia.its.canvas.roster.model.RosterManagementForm;
import edu.virginia.its.canvas.roster.service.RosterManagementService;
import edu.virginia.its.canvas.roster.utils.Constants;
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

  @GetMapping("/")
  public String index(Model model) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);

    Course currentCourse = rosterManagementService.getCourse(courseId);
    model.addAttribute("courseName", currentCourse.name());
    model.addAttribute("courseCode", currentCourse.courseCode());
    model.addAttribute("courseSisId", currentCourse.sisCourseId());
    List<Section> currentCourseSections = rosterManagementService.getValidCourseSections(courseId);
    model.addAttribute("currentCourseSections", currentCourseSections);

    List<Course> userCourses = rosterManagementService.getUserCourses(computingId);
    Map<Term, List<Section>> sectionsMap = rosterManagementService.getAllUserSections(userCourses);
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

  @PostMapping("/validate")
  public String validate(Model model, @ModelAttribute RosterManagementForm rosterManagementForm) {
    CanvasAuthenticationToken token = CanvasAuthenticationToken.getToken();
    String courseId = token.getCustomValue(Constants.COURSE_ID_CUSTOM_KEY);
    String computingId = token.getCustomValue(Constants.USERNAME_CUSTOM_KEY);
    List<Section> currentCourseSections = rosterManagementService.getValidCourseSections(courseId);
    // Don't try to remove sections that have a null crosslisted course id (meaning that they were
    // originally created in the current course).
    List<Section> sectionsToRemove =
        currentCourseSections.stream()
            .filter(
                section ->
                    section.crosslistedCourseId() != null
                        && !rosterManagementForm.getSectionsToKeep().contains(section.id()))
            .toList();
    model.addAttribute("sectionsToRemove", sectionsToRemove);

    List<Course> userCourses = rosterManagementService.getUserCourses(computingId);
    Map<Term, List<Section>> sectionsMap = rosterManagementService.getAllUserSections(userCourses);
    List<Section> allSections = sectionsMap.values().stream().flatMap(List::stream).toList();
    List<Section> sectionsToAdd =
        allSections.stream()
            .filter(section -> rosterManagementForm.getSectionsToAdd().contains(section.id()))
            .toList();
    model.addAttribute("sectionsToAdd", sectionsToAdd);
    model.addAttribute("rosterManagementForm", rosterManagementForm);

    List<Course> coursesToRemoveUserFrom =
        userCourses.stream()
            .filter(
                course ->
                    sectionsToAdd.stream()
                        .anyMatch(section -> section.courseId().equals(course.id())))
            .toList();
    model.addAttribute("coursesToRemoveUserFrom", coursesToRemoveUserFrom);
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
        currentCourseSections.stream()
            .filter(
                section ->
                    section.crosslistedCourseId() != null
                        && !rosterManagementForm.getSectionsToKeep().contains(section.id()))
            .toList();
    List<Section> sectionsToRemoveErrors = new ArrayList<>();
    for (Section sectionToRemove : sectionsToRemove) {
      log.info(
          "Decrosslisting section '{}' back to course '{}'",
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
    Map<Term, List<Section>> sectionsMap = rosterManagementService.getAllUserSections(userCourses);
    List<Section> allSections = sectionsMap.values().stream().flatMap(List::stream).toList();
    List<Section> sectionsToAdd =
        allSections.stream()
            .filter(section -> rosterManagementForm.getSectionsToAdd().contains(section.id()))
            .toList();
    List<Section> sectionsToAddErrors = new ArrayList<>();
    Set<String> coursesToRemoveUserFrom = new HashSet<>();
    for (Section sectionToAdd : sectionsToAdd) {
      log.info("Crosslisting section '{}' to course '{}'", sectionToAdd, courseId);
      boolean success = rosterManagementService.crosslistSection(sectionToAdd, courseId);
      if (!success) {
        sectionsToAddErrors.add(sectionToAdd);
        continue;
      }
      if (rosterManagementForm.isRemoveFromCourse()) {
        coursesToRemoveUserFrom.add(sectionToAdd.courseId());
      }
    }
    for (String courseToRemoveUserFromId : coursesToRemoveUserFrom) {
      log.info("Removing user '{}' from course '{}'", computingId, courseToRemoveUserFromId);
      rosterManagementService.removeUserFromCourse(computingId, courseToRemoveUserFromId);
    }
    model.addAttribute("sectionsToAdd", sectionsToAdd);
    model.addAttribute("sectionsToAddErrors", sectionsToAddErrors);
    return "success";
  }
}

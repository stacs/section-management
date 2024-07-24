package edu.virginia.its.canvas.section.service;

import edu.virginia.its.canvas.section.api.CanvasApi;
import edu.virginia.its.canvas.section.model.BoomiResponses.SisSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.Course;
import edu.virginia.its.canvas.section.model.CanvasResponses.Enrollment;
import edu.virginia.its.canvas.section.model.CanvasResponses.Term;
import edu.virginia.its.canvas.section.model.SectionDTO;
import edu.virginia.its.canvas.section.utils.SectionMapper;
import edu.virginia.its.canvas.section.utils.SectionUtils;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
public class SectionManagementService {

  private final CanvasApi canvasApi;
  private final WaitlistedSectionService waitlistedSectionService;
  private final SectionMapper sectionMapper;

  public SectionManagementService(
      CanvasApi canvasApi,
      WaitlistedSectionService waitlistedSectionService,
      SectionMapper sectionMapper) {
    this.canvasApi = canvasApi;
    this.waitlistedSectionService = waitlistedSectionService;
    this.sectionMapper = sectionMapper;
  }

  public Course getCourse(String courseId) {
    return canvasApi.getCourse(courseId);
  }

  public List<SectionDTO> getUsersTeachingSections(List<Course> userCourses) {
    List<SectionDTO> sectionDTOS = new ArrayList<>();
    // Both these maps are used to store term lookups so we can tell what term a section is
    // associated with.
    // TODO: might be better to get the terms via the Terms API?
    Map<String, Term> courseSisIdToTermMap = new HashMap<>();
    Map<String, Term> termSisIdToTermMap = new HashMap<>();
    for (Course course : userCourses) {
      courseSisIdToTermMap.put(course.sisCourseId(), course.term());
      termSisIdToTermMap.put(course.term().sisTermId(), course.term());
    }
    List<CanvasSection> canvasSections = getCanvasSectionsForCourses(userCourses);
    List<SisSection> sisSections =
        waitlistedSectionService.getWaitlistStatusForSections(canvasSections);
    Map<String, SisSection> sectionSisIdToSisSectionMap =
        sisSections.stream()
            .collect(Collectors.toMap(SisSection::getSisSectionId, Function.identity()));
    canvasSections.sort(Comparator.comparing(CanvasSection::sisSectionId));
    sisSections.sort(Comparator.comparing(SisSection::getSisSectionId));
    for (CanvasSection canvasSection : canvasSections) {
      SisSection sisSection = sectionSisIdToSisSectionMap.get(canvasSection.sisSectionId());
      Term term = null;
      // The first part of both Charlottesville and Wise sections is the term
      if (canvasSection.sisSectionId() != null) {
        String[] sectionTerm = canvasSection.sisSectionId().split("_");
        if (sectionTerm.length > 0) {
          term = termSisIdToTermMap.get(sectionTerm[0]);
        }
      }
      if (term == null) {
        term = courseSisIdToTermMap.get(canvasSection.sisCourseId());
      }
      SectionDTO sectionDTO = sectionMapper.from(canvasSection, sisSection, term);
      sectionDTOS.add(sectionDTO);
    }
    return sectionDTOS;
  }

  public List<Course> getUserCourses(String computingId) {
    // Need to remove courses that arent valid, maybe by using isValidTerm
    List<Course> courses = canvasApi.getUsersTeachingCourses(computingId);
    // Remove duplicate courses from the list, because the way the Canvas API works
    // we pull the users Teacher courses and then the users TA courses so there may be
    // duplicates we need to trim
    return courses.stream().distinct().toList();
  }

  public List<CanvasSection> getCanvasSectionsForCourses(List<Course> userCourses) {
    List<CanvasSection> allCanvasSections = new ArrayList<>();
    // TODO: try to use spring reactive to make these calls simultaneously
    for (Course course : userCourses) {
      if (!isValidTerm(course.term())) {
        continue;
      }
      List<CanvasSection> canvasSections = canvasApi.getCourseSections(course.id());
      List<CanvasSection> validCanvasSections = SectionUtils.getValidSisSections(canvasSections);
      allCanvasSections.addAll(validCanvasSections);
    }
    return allCanvasSections;
  }

  public boolean crosslistSection(SectionDTO section, String newCourseId) {
    try {
      canvasApi.crosslistSection(section.getId(), newCourseId);
    } catch (Exception ex) {
      return false;
    }
    return true;
  }

  public boolean deCrosslistSection(SectionDTO section) {
    try {
      canvasApi.deCrosslistSection(section.getId());
    } catch (Exception ex) {
      return false;
    }
    return true;
  }

  public boolean removeUserFromCourse(String computingId, String courseId) {
    List<Enrollment> enrollments = canvasApi.getCourseEnrollments(courseId);
    for (Enrollment enrollment : enrollments) {
      if (("TeacherEnrollment".equals(enrollment.role())
              || "TaEnrollment".equals(enrollment.role()))
          && computingId.equals(enrollment.sisUserId())) {
        try {
          canvasApi.deleteUserEnrollment(courseId, enrollment.id());
        } catch (Exception ex) {
          log.error(
              "There was an error attempting to remove user '{}' from course '{}'",
              computingId,
              courseId,
              ex);
          return false;
        }
      }
    }
    return true;
  }

  private boolean isValidTerm(Term term) {
    // Ignore Terms without a SIS ID and Terms that have a SIS ID but aren't actual terms (such
    // as Training/Admin Term which has a SIS ID of 9999).
    String sisTermId = term.sisTermId();
    if (ObjectUtils.isEmpty(sisTermId)) {
      return false;
    }
    return !sisTermId.equals("9999") && !sisTermId.equals("9997");
  }
}

package edu.virginia.its.canvas.section.service;

import edu.virginia.its.canvas.section.api.CanvasApi;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.Course;
import edu.virginia.its.canvas.section.model.CanvasResponses.Enrollment;
import edu.virginia.its.canvas.section.model.CanvasResponses.Term;
import edu.virginia.its.canvas.section.utils.SectionUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
public class SectionManagementService {

  private final CanvasApi canvasApi;

  public SectionManagementService(CanvasApi canvasApi) {
    this.canvasApi = canvasApi;
  }

  public Course getCourse(String courseId) {
    return canvasApi.getCourse(courseId);
  }

  public List<Course> getUserCourses(String computingId) {
    return canvasApi.getUserCourses(computingId);
  }

  public List<CanvasSection> getValidCourseSections(String courseId) {
    List<CanvasSection> canvasSections =
        SectionUtils.getValidSisSections(canvasApi.getCourseSections(courseId));
    canvasSections.sort(SectionUtils.SECTION_NAME_COMPARATOR);
    return canvasSections;
  }

  public Map<Term, List<CanvasSection>> getAllUserSectionsGroupedByTerm(List<Course> userCourses) {
    // Sort terms by most recent
    Map<Term, List<CanvasSection>> sectionsMap =
        new TreeMap<>(Comparator.comparing(Term::sisTermId, Comparator.reverseOrder()));
    List<CanvasSection> allCanvasSections = getAllUserSections(userCourses);
    for (CanvasSection canvasSection : allCanvasSections) {
      // Unfortunately the Section object that comes from Canvas does not include the Term, so we
      // need to associate
      // the Course to the Section in order to determine what the Section's Term is.
      Course course =
          userCourses.stream()
              .filter(c -> canvasSection.courseId().equals(c.id()))
              .findFirst()
              .orElse(null);
      if (course == null) {
        log.warn("Could not find course object for section '{}'", canvasSection);
        continue;
      }
      sectionsMap.computeIfAbsent(course.term(), k -> new ArrayList<>());
      sectionsMap.get(course.term()).add(canvasSection);
    }
    sectionsMap.values().removeIf(List::isEmpty);
    // Sort sections within a term by name
    sectionsMap
        .values()
        .forEach(sectionList -> sectionList.sort(SectionUtils.SECTION_NAME_COMPARATOR));
    return sectionsMap;
  }

  public List<CanvasSection> getAllUserSections(List<Course> userCourses) {
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

  public boolean crosslistSection(CanvasSection canvasSection, String newCourseId) {
    try {
      canvasApi.crosslistSection(canvasSection.id(), newCourseId);
    } catch (Exception ex) {
      return false;
    }
    return true;
  }

  public boolean deCrosslistSection(CanvasSection canvasSection) {
    try {
      canvasApi.deCrosslistSection(canvasSection.id());
    } catch (Exception ex) {
      return false;
    }
    return true;
  }

  public boolean removeUserFromCourse(String computingId, String courseId) {
    List<Enrollment> enrollments = canvasApi.getCourseEnrollments(courseId);
    for (Enrollment enrollment : enrollments) {
      if ("TeacherEnrollment".equals(enrollment.role())
          && computingId.equals(enrollment.sisUserId())) {
        try {
          canvasApi.deleteUserEnrollment(courseId, enrollment.id());
        } catch (Exception ex) {
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
    try {
      int sisTermIdInt = Integer.parseInt(sisTermId);
      if (sisTermIdInt >= 2000) {
        return false;
      }
    } catch (NumberFormatException ex) {
      log.warn("Error while parsing sisTermId '{}'", sisTermId, ex);
      return false;
    }
    return true;
  }
}

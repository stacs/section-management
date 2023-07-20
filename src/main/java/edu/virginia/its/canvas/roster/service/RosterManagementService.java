package edu.virginia.its.canvas.roster.service;

import edu.virginia.its.canvas.roster.api.CanvasApi;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Course;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Enrollment;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Section;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Term;
import edu.virginia.its.canvas.roster.utils.SectionUtils;
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
public class RosterManagementService {

  private final CanvasApi canvasApi;

  public RosterManagementService(CanvasApi canvasApi) {
    this.canvasApi = canvasApi;
  }

  public Course getCourse(String courseId) {
    return canvasApi.getCourse(courseId);
  }

  public List<Course> getUserCourses(String computingId) {
    return canvasApi.getUserCourses(computingId);
  }

  public List<Section> getValidCourseSections(String courseId) {
    List<Section> sections =
        SectionUtils.getValidSisSections(canvasApi.getCourseSections(courseId));
    SectionUtils.sortSectionsByCrosslistingThenName(sections);
    return sections;
  }

  public Map<Term, List<Section>> getAllUserSectionsGroupedByTerm(List<Course> userCourses) {
    // Sort terms by most recent
    Map<Term, List<Section>> sectionsMap =
        new TreeMap<>(Comparator.comparing(Term::sisTermId, Comparator.reverseOrder()));
    List<Section> allSections = getAllUserSections(userCourses);
    for (Section section : allSections) {
      // Unfortunately the Section object that comes from Canvas does not include the Term, so we
      // need to associate
      // the Course to the Section in order to determine what the Section's Term is.
      Course course =
          userCourses.stream()
              .filter(c -> section.courseId().equals(c.id()))
              .findFirst()
              .orElse(null);
      if (course == null) {
        log.warn("Could not find course object for section '{}'", section);
        continue;
      }
      sectionsMap.computeIfAbsent(course.term(), k -> new ArrayList<>());
      sectionsMap.get(course.term()).add(section);
    }
    sectionsMap.values().removeIf(List::isEmpty);
    // Sort sections within a term by name
    sectionsMap.values().forEach(SectionUtils::sortSectionsByName);
    return sectionsMap;
  }

  public List<Section> getAllUserSections(List<Course> userCourses) {
    List<Section> allSections = new ArrayList<>();
    // TODO: try to use spring reactive to make these calls simultaneously
    for (Course course : userCourses) {
      if (!isValidTerm(course.term())) {
        continue;
      }
      List<Section> sections = canvasApi.getCourseSections(course.id());
      List<Section> validSections = SectionUtils.getValidSisSections(sections);
      allSections.addAll(validSections);
    }
    return allSections;
  }

  public boolean crosslistSection(Section section, String newCourseId) {
    try {
      canvasApi.crosslistSection(section.id(), newCourseId);
    } catch (Exception ex) {
      return false;
    }
    return true;
  }

  public boolean deCrosslistSection(Section section) {
    try {
      canvasApi.deCrosslistSection(section.id());
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

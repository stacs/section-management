package edu.virginia.its.canvas.roster.service;

import edu.virginia.its.canvas.roster.api.CanvasApi;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Course;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Enrollment;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Section;
import edu.virginia.its.canvas.roster.model.CanvasResponses.Term;
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

  private final Comparator<Section> alreadyAddedSectionsComparator;

  private final Comparator<Section> sectionNameComparator;

  public RosterManagementService(CanvasApi canvasApi) {
    this.canvasApi = canvasApi;
    this.alreadyAddedSectionsComparator =
        Comparator.comparing(Section::isCrosslisted, Comparator.naturalOrder())
            .thenComparing(Section::name, Comparator.naturalOrder());
    this.sectionNameComparator = Comparator.comparing(Section::name);
  }

  public Course getCourse(String courseId) {
    return canvasApi.getCourse(courseId);
  }

  public List<Course> getUserCourses(String computingId) {
    return canvasApi.getUserCourses(computingId);
  }

  public List<Section> getValidCourseSections(String courseId) {
    List<Section> sections = getValidSisSections(canvasApi.getCourseSections(courseId));
    return sections.stream().sorted(alreadyAddedSectionsComparator).toList();
  }

  public Map<Term, List<Section>> getAllUserSections(List<Course> userCourses) {
    // Sort terms by most recent
    Map<Term, List<Section>> sectionsMap =
        new TreeMap<>(Comparator.comparing(Term::sisTermId, Comparator.reverseOrder()));
    // TODO: try to use spring reactive to make these calls simultaneously
    for (Course course : userCourses) {
      if (!isValidTerm(course.term())) {
        continue;
      }
      sectionsMap.computeIfAbsent(course.term(), k -> new ArrayList<>());
      List<Section> sections = canvasApi.getCourseSections(course.id());
      List<Section> validSections = getValidSisSections(sections);
      sectionsMap.get(course.term()).addAll(validSections);
    }
    sectionsMap.values().removeIf(List::isEmpty);
    // Sort sections within a term by name
    sectionsMap.values().forEach(this::sortSectionsByName);
    return sectionsMap;
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

  public List<Section> getValidSisSections(List<Section> sections) {
    return sections.stream().filter(s -> !ObjectUtils.isEmpty(s.sisSectionId())).toList();
  }

  public List<Section> getSectionsCreatedInCourse(String courseId, List<Section> sections) {
    return sections.stream()
        .filter(
            section ->
                (courseId.equals(section.courseId()) && section.crosslistedCourseId() == null)
                    || courseId.equals(section.crosslistedCourseId()))
        .toList();
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

  public void sortSectionsByName(List<Section> sections) {
    sections.sort(sectionNameComparator);
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

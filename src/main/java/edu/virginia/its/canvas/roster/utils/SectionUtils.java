package edu.virginia.its.canvas.roster.utils;

import edu.virginia.its.canvas.roster.model.CanvasResponses.Section;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.util.ObjectUtils;

public final class SectionUtils {

  private static final Comparator<Section> ALREADY_ADDED_SECTIONS_COMPARATOR =
      Comparator.comparing(Section::isCrosslisted, Comparator.naturalOrder())
          .thenComparing(Section::name, Comparator.naturalOrder());

  private static final Comparator<Section> SECTION_NAME_COMPARATOR =
      Comparator.comparing(Section::name);

  public static List<Section> getValidSisSections(List<Section> sections) {
    return new ArrayList<>(
        sections.stream().filter(s -> !ObjectUtils.isEmpty(s.sisSectionId())).toList());
  }

  public static List<Section> getSectionsCreatedInCourse(String courseId, List<Section> sections) {
    return new ArrayList<>(
        sections.stream()
            .filter(
                section ->
                    (courseId.equals(section.courseId()) && section.crosslistedCourseId() == null)
                        || courseId.equals(section.crosslistedCourseId()))
            .toList());
  }

  public static void sortSectionsByName(List<Section> sections) {
    sections.sort(SECTION_NAME_COMPARATOR);
  }

  public static void sortSectionsByCrosslistingThenName(List<Section> sections) {
    sections.sort(ALREADY_ADDED_SECTIONS_COMPARATOR);
  }
}

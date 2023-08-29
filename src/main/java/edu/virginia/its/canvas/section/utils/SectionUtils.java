package edu.virginia.its.canvas.section.utils;

import edu.virginia.its.canvas.section.model.CanvasResponses.Section;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.util.ObjectUtils;

public final class SectionUtils {

  public static final Comparator<Section> ALREADY_ADDED_SECTIONS_COMPARATOR =
      Comparator.comparing(Section::isCrosslisted, Comparator.naturalOrder())
          .thenComparing(Section::name, Comparator.naturalOrder());

  public static final Comparator<Section> SECTION_NAME_COMPARATOR =
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
}

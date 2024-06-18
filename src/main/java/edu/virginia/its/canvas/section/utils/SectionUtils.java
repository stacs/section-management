package edu.virginia.its.canvas.section.utils;

import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlist;
import edu.virginia.its.canvas.section.model.BoomiRequests.CanvasWaitlistStatus;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.SectionDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.util.ObjectUtils;

public final class SectionUtils {

  private SectionUtils() {}

  public static final Comparator<SectionDTO> ALREADY_ADDED_SECTIONS_COMPARATOR =
      Comparator.comparing(SectionDTO::isCrosslisted, Comparator.naturalOrder())
          .thenComparing(SectionDTO::getName, Comparator.naturalOrder());

  public static final Comparator<SectionDTO> SECTION_NAME_COMPARATOR =
      Comparator.comparing(SectionDTO::getName);

  public static List<CanvasSection> getValidSisSections(List<CanvasSection> canvasSections) {
    return new ArrayList<>(
        canvasSections.stream().filter(s -> !ObjectUtils.isEmpty(s.sisSectionId())).toList());
  }

  public static List<CanvasSection> getSectionsCreatedInCourse(
      String courseId, List<CanvasSection> canvasSections) {
    return new ArrayList<>(
        canvasSections.stream()
            .filter(
                section ->
                    (courseId.equals(section.courseId()) && section.crosslistedCourseId() == null)
                        || courseId.equals(section.crosslistedCourseId()))
            .toList());
  }

  public static CanvasWaitlistStatus sectionIdToCanvasWaitlistStatus(String sisSectionId) {
    // TODO: are there other format types we need to handle?
    String[] sectionParts = sisSectionId.split("_");
    if (sectionParts.length == 4) {
      String[] courseParts = sectionParts[2].split("-");
      if (courseParts.length == 2) {
        return new CanvasWaitlistStatus(
            sectionParts[0], sectionParts[1], courseParts[0], courseParts[1], sectionParts[3]);
      }
    }
    return null;
  }

  public static CanvasWaitlist sectionIdToCanvasWaitlist(String sisSectionId, Boolean waitlist) {
    // TODO: are there other format types we need to handle?
    String[] sectionParts = sisSectionId.split("_");
    if (sectionParts.length == 4) {
      String[] courseParts = sectionParts[2].split("-");
      if (courseParts.length == 2) {
        return new CanvasWaitlist(
            sectionParts[0],
            sectionParts[1],
            courseParts[0],
            courseParts[1],
            sectionParts[3],
            waitlist);
      }
    }
    return null;
  }
}

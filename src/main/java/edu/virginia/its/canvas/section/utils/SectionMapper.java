package edu.virginia.its.canvas.section.utils;

import edu.virginia.its.canvas.section.model.BoomiResponses.SisSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.CanvasSection;
import edu.virginia.its.canvas.section.model.CanvasResponses.Term;
import edu.virginia.its.canvas.section.model.SectionDTO;
import edu.virginia.its.canvas.section.model.TermDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class SectionMapper {

  /**
   * Combines section data from two APIs into a single section object usable by the template
   *
   * @param canvasSection the section data from the Canvas API
   * @param sisSection the section data from the Boomi API
   * @return the combined section data
   */
  public SectionDTO from(CanvasSection canvasSection, SisSection sisSection, Term term) {
    SectionDTO sectionDTO = new SectionDTO();
    sectionDTO.setId(canvasSection.id());
    sectionDTO.setSisId(canvasSection.sisSectionId());
    sectionDTO.setName(canvasSection.name());
    sectionDTO.setCourseId(canvasSection.courseId());
    sectionDTO.setCourseSisId(canvasSection.sisCourseId());
    TermDTO termDTO = new TermDTO(term.sisTermId(), term.name());
    sectionDTO.setTerm(termDTO);
    sectionDTO.setTotalStudents(canvasSection.totalStudents());
    sectionDTO.setCrosslisted(!ObjectUtils.isEmpty(sectionDTO.getCrosslistedCourseId()));
    sectionDTO.setCrosslistedCourseId(sectionDTO.getCrosslistedCourseId());
    sectionDTO.setWaitlist(sisSection.hasWaitlist());
    sectionDTO.setWaitlistStudents(sisSection.numberOfWaitlistStudents());
    return sectionDTO;
  }
}

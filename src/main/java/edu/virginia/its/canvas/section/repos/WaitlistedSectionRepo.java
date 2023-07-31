package edu.virginia.its.canvas.section.repos;

import edu.virginia.its.canvas.section.model.WaitlistedSection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistedSectionRepo extends JpaRepository<WaitlistedSection, Long> {
  List<WaitlistedSection> findBySisSectionIdIn(List<String> sections);
}

package edu.virginia.its.canvas.roster.model;

import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "section_management_waitlists")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class WaitlistedSection {

  @Id
  @Column(nullable = false)
  private Long canvasId;

  @Column(nullable = false)
  private String sisSectionId;

  @Column(nullable = false)
  private boolean waitlisted;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private OffsetDateTime dateCreated;

  @LastModifiedDate
  @Column(nullable = false)
  private OffsetDateTime lastUpdated;
}

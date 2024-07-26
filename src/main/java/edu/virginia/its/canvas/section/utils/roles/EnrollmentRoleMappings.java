package edu.virginia.its.canvas.section.utils.roles;

import edu.virginia.its.canvas.lti.util.Constants;
import edu.virginia.its.canvas.lti.util.RolesMap;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public final class EnrollmentRoleMappings {

  @Bean
  public RolesMap roleMappings() {
    RolesMap roleMappings = new RolesMap();
    roleMappings.put(
        edu.virginia.its.canvas.section.utils.Constants.TEACHER_ENROLLMENT,
        Constants.INSTRUCTOR_ROLE);
    roleMappings.put(
        edu.virginia.its.canvas.section.utils.Constants.TA_ENROLLMENT, Constants.TA_ROLE);
    roleMappings.put(
        edu.virginia.its.canvas.section.utils.Constants.ACCOUNT_ADMIN, Constants.ADMIN_ROLE);
    roleMappings.put(
        edu.virginia.its.canvas.section.utils.Constants.SUBACCOUNT_ADMIN, Constants.ADMIN_ROLE);
    roleMappings.put(
        edu.virginia.its.canvas.section.utils.Constants.STUDENT_ENROLLMENT, Constants.STUDENT_ROLE);
    roleMappings.put(
        edu.virginia.its.canvas.section.utils.Constants.WAITLISTED_STUDENT, Constants.STUDENT_ROLE);
    roleMappings.put(edu.virginia.its.canvas.section.utils.Constants.LIBRARIAN, "ROLE_LIBRARIAN");
    roleMappings.put(
        edu.virginia.its.canvas.section.utils.Constants.DESIGNER_ENROLLMENT, "ROLE_DESIGNER");
    roleMappings.put(
        edu.virginia.its.canvas.section.utils.Constants.OBSERVER_ENROLLMENT, "ROLE_OBSERVER");
    return roleMappings;
  }
}

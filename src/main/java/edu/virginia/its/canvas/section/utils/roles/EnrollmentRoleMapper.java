package edu.virginia.its.canvas.section.utils.roles;

import com.nimbusds.jose.shaded.json.JSONObject;
import edu.virginia.its.canvas.lti.util.RolesMap;
import edu.virginia.its.canvas.section.utils.Constants;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import uk.ac.ox.ctl.lti13.lti.Claims;

/**
 * A GrantedAuthoritiesMapper that uses the Canvas membership roles to determine role mappings. This
 * is used so we can better determine access when given custom roles inside of Canvas. For example,
 * with the normal LTI role mapping there would be no difference between a Teacher and a role that
 * was extended from Teacher (such as Librarian).
 */
@Slf4j
@Component
public class EnrollmentRoleMapper implements GrantedAuthoritiesMapper {
  private final RolesMap roleMappings;

  public EnrollmentRoleMapper(RolesMap roleMappings) {
    this.roleMappings = roleMappings;
    log.info("Using the following role mappings: {}", roleMappings);
  }

  @Override
  public Collection<? extends GrantedAuthority> mapAuthorities(
      Collection<? extends GrantedAuthority> authorities) {
    Set<GrantedAuthority> newAuthorities = new HashSet<>(authorities);
    for (GrantedAuthority authority : authorities) {
      OidcUserAuthority userAuth = (OidcUserAuthority) authority;
      Object customClaims = userAuth.getAttributes().get(Claims.CUSTOM);
      if (customClaims instanceof JSONObject customJson) {
        String enrollmentRoles = customJson.getAsString(Constants.ROLES_CUSTOM_KEY);
        if (!ObjectUtils.isEmpty(enrollmentRoles)) {
          String[] splitEnrollmentRoles = enrollmentRoles.split(",");
          for (String splitEnrollmentRole : splitEnrollmentRoles) {
            String newRole = roleMappings.get(splitEnrollmentRole);
            if (newRole != null) {
              newAuthorities.add(
                  new OidcUserAuthority(newRole, userAuth.getIdToken(), userAuth.getUserInfo()));
            }
          }
        }
      }
    }
    return newAuthorities;
  }
}

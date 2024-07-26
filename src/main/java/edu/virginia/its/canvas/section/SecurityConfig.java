package edu.virginia.its.canvas.section;

import edu.virginia.its.canvas.section.utils.roles.EnrollmentRoleMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import uk.ac.ox.ctl.lti13.Lti13Configurer;

@EnableWebSecurity
public class SecurityConfig {

  private final EnrollmentRoleMapper enrollmentRoleMapper;

  public SecurityConfig(EnrollmentRoleMapper enrollmentRoleMapper) {
    this.enrollmentRoleMapper = enrollmentRoleMapper;
  }

  @Bean
  protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.authorizeRequests()
        .antMatchers(
            "/resources/**",
            "/favicon.ico",
            "/config.json",
            "/.well-known/jwks.json",
            "/lti/login",
            "/icons/**",
            "/error")
        .permitAll()
        .antMatchers("/**")
        .access(
            "!hasAnyRole('STUDENT', 'OBSERVER', 'LIBRARIAN', 'DESIGNER') and hasAnyRole('INSTRUCTOR', 'TA')");
    Lti13Configurer lti13Configurer =
        new Lti13Configurer().grantedAuthoritiesMapper(enrollmentRoleMapper);
    http.apply(lti13Configurer);
    return http.build();
  }
}

package edu.virginia.its.canvas.section;

import edu.virginia.its.canvas.lti.roles.CanvasRoleMapper;
import edu.virginia.its.canvas.lti.util.AuthorizationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import uk.ac.ox.ctl.lti13.Lti13Configurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final CanvasRoleMapper canvasRoleMapper;

  public SecurityConfig(CanvasRoleMapper canvasRoleMapper) {
    this.canvasRoleMapper = canvasRoleMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, ApplicationContext context)
      throws Exception {
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers(
                    "/resources/**",
                    "/favicon.ico",
                    "/config.json",
                    "/.well-known/jwks.json",
                    "/lti/login",
                    "/icons/**",
                    "/error")
                .permitAll()
                .requestMatchers("/**")
                .access(
                    AuthorizationUtils.getAuthz(
                        "!hasAnyRole('STUDENT', 'OBSERVER', 'LIBRARIAN', 'DESIGNER') and hasAnyRole('INSTRUCTOR', 'TA')",
                        context)));
    Lti13Configurer lti13Configurer =
        new Lti13Configurer().grantedAuthoritiesMapper(canvasRoleMapper);
    lti13Configurer.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
    http.with(lti13Configurer, Customizer.withDefaults());
    return http.build();
  }
}

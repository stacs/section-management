package edu.virginia.its.canvas.section;

import edu.virginia.its.canvas.lti.util.RoleMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import uk.ac.ox.ctl.lti13.Lti13Configurer;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final RoleMapper roleMapper;

  public SecurityConfig(RoleMapper roleMapper) {
    this.roleMapper = roleMapper;
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
        .hasRole("INSTRUCTOR");
    Lti13Configurer lti13Configurer = new Lti13Configurer().grantedAuthoritiesMapper(roleMapper);
    http.apply(lti13Configurer);
    return http.build();
  }
}

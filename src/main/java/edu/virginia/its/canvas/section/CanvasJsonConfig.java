package edu.virginia.its.canvas.section;

import static edu.virginia.its.canvas.lti.util.Constants.CANVAS_ROLES_CUSTOM_KEY;

import edu.virginia.its.canvas.section.utils.Constants;
import edu.virginia.lts.canvas.Config;
import edu.virginia.lts.canvas.Extension;
import edu.virginia.lts.canvas.Placement;
import edu.virginia.lts.canvas.Settings;
import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CanvasJsonConfig {

  @Value("${ltitool.baseUrl}")
  private String baseUrl;

  @Value("${ltitool.toolName}")
  private String toolName;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Bean
  public Config getConfig() throws MalformedURLException {
    String domain = new URL(baseUrl).getHost();
    String appUrl = baseUrl + contextPath;

    Placement courseSettingsSubNavigation =
        Placement.builder()
            .placement("course_settings_sub_navigation")
            .messageType(Placement.LTI_RESOURCE_LINK_REQUEST)
            .customField(Constants.USERNAME_CUSTOM_KEY, "$Canvas.user.sisSourceId")
            .customField(Constants.COURSE_ID_CUSTOM_KEY, "$Canvas.course.id")
            .customField(CANVAS_ROLES_CUSTOM_KEY, "$Canvas.membership.roles")
            // Add a space to the start of the text so the icon and text are spaced apart a bit.
            .text(" Manage SIS Sections")
            .iconUrl(appUrl + "/icons/users-solid.svg")
            .build();
    Settings settings = Settings.builder().placement(courseSettingsSubNavigation).build();
    Extension extension =
        Extension.builder()
            .domain(domain)
            .toolId(toolName)
            .platform(Extension.CANVAS_PLATFORM)
            .privacyLevel(Extension.PUBLIC)
            .settings(settings)
            .build();
    return Config.builder()
        .title("Section Management")
        .description("Manage SIS Sections")
        .oidcInitiationUrl(appUrl + "/lti/login_initiation/" + toolName)
        .targetLinkUri(appUrl + "/launch")
        .scope(Config.LINEITEM)
        .scope(Config.RESULT_READONLY)
        .extension(extension)
        .publicJwkUrl(appUrl + "/.well-known/jwks.json")
        .build();
  }
}

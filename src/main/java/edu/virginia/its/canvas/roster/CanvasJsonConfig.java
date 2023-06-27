package edu.virginia.its.canvas.roster;

import edu.virginia.its.canvas.roster.utils.Constants;
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

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @Bean
  public Config getConfig() throws MalformedURLException {
    String toolName = this.getClass().getPackage().getImplementationTitle();
    String domain = new URL(baseUrl).getHost();
    String appUrl = baseUrl + contextPath;

    Placement courseSettingsSubNavigation =
        Placement.builder()
            .placement("course_settings_sub_navigation")
            .messageType(Placement.LTI_RESOURCE_LINK_REQUEST)
            .customField(Constants.USERNAME_CUSTOM_KEY, "$Canvas.user.sisSourceId")
            .customField(Constants.COURSE_ID_CUSTOM_KEY, "$Canvas.course.id")
            .text("Manage SIS Sections")
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
        .title("Roster Management")
        .description("Roster Management")
        .oidcInitiationUrl(appUrl + "/lti/login_initiation/" + toolName)
        .targetLinkUri(appUrl + "/")
        .scope(Config.LINEITEM)
        .scope(Config.RESULT_READONLY)
        .extension(extension)
        .publicJwkUrl(appUrl + "/.well-known/jwks.json")
        .build();
  }
}

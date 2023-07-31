package edu.virginia.its.canvas.section;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("edu.virginia.its.canvas.section.repos")
@EntityScan("edu.virginia.its.canvas.section.model")
public class SectionManagementApplication {

  public static void main(String[] args) {
    SpringApplication.run(SectionManagementApplication.class, args);
  }
}

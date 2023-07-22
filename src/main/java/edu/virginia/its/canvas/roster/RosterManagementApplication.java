package edu.virginia.its.canvas.roster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("edu.virginia.its.canvas.roster.repos")
@EntityScan("edu.virginia.its.canvas.roster.model")
public class RosterManagementApplication {

  public static void main(String[] args) {
    SpringApplication.run(RosterManagementApplication.class, args);
  }
}

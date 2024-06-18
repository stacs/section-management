package edu.virginia.its.canvas.section.service;

import edu.virginia.its.canvas.section.SectionManagementApplication;
import edu.virginia.its.canvas.section.SecurityConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {SectionManagementApplication.class, SecurityConfig.class})
class SectionManagementServiceTest {}

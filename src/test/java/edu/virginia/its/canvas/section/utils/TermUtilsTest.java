package edu.virginia.its.canvas.section.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TermUtilsTest {

  @Test
  void testGetCurrentTerm() {
    assertEquals("1218", TermUtils.getCurrentTerm(LocalDate.of(2022, 1, 6)));
    assertEquals("1231", TermUtils.getCurrentTerm(LocalDate.of(2023, 2, 19)));
    assertEquals("1252", TermUtils.getCurrentTerm(LocalDate.of(2025, 3, 3)));
    assertEquals("1082", TermUtils.getCurrentTerm(LocalDate.of(2008, 4, 25)));
    assertEquals("1242", TermUtils.getCurrentTerm(LocalDate.of(2024, 5, 9)));
    assertEquals("1232", TermUtils.getCurrentTerm(LocalDate.of(2023, 6, 30)));
    assertEquals("1236", TermUtils.getCurrentTerm(LocalDate.of(2023, 7, 2)));
    assertEquals("1226", TermUtils.getCurrentTerm(LocalDate.of(2022, 8, 17)));
    assertEquals("1246", TermUtils.getCurrentTerm(LocalDate.of(2024, 9, 1)));
    assertEquals("1248", TermUtils.getCurrentTerm(LocalDate.of(2024, 10, 13)));
    assertEquals("1138", TermUtils.getCurrentTerm(LocalDate.of(2013, 11, 15)));
    assertEquals("1238", TermUtils.getCurrentTerm(LocalDate.of(2023, 12, 25)));
  }
}

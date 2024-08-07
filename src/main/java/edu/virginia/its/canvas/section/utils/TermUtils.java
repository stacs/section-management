package edu.virginia.its.canvas.section.utils;

import java.time.LocalDate;
import java.time.Month;

public final class TermUtils {
  private TermUtils() {}

  /**
   * Returns the 4 digit SIS Term ID that we are currently in. This also includes an offset so we
   * can match as best as possible when SIS switches what Terms they send us. Since currently in
   * Canvas we don't have access to the end date of a Term we generalize things so that we cover an
   * offset of at least 28 days since term-end-date + 28 days is when SIS switches the Terms they
   * send us.
   *
   * @param date the date to use to determine what Term we are currently in.
   * @return the 4 digit SIS Term ID for the Term we are currently in.
   */
  public static String getCurrentTerm(LocalDate date) {
    int twoDigitYear = date.getYear() % 100;
    String semester =
        switch (date.getMonth()) {
          case FEBRUARY -> "1";
          case MARCH, APRIL, MAY, JUNE -> "2";
          case JULY, AUGUST, SEPTEMBER -> "6";
          case OCTOBER, NOVEMBER, DECEMBER, JANUARY -> "8";
        };
    // If we are in January then we want to use the previous year's Fall term
    if (date.getMonth() == Month.JANUARY) {
      twoDigitYear--;
    }
    return "1" + String.format("%02d", twoDigitYear) + semester;
  }
}

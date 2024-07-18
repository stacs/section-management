package edu.virginia.its.canvas.section.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TermDTO implements Comparable<TermDTO> {
  private String sisId;
  private String name;

  /**
   * CompareTo method, we want to display the terms so that they are grouped by Charlottesville
   * first then Wise, and within each group they are ordered by most recent to least recent
   */
  @Override
  public int compareTo(TermDTO other) {
    // Charlottesville SIS IDs are 4 digits, Wise are 6 characters
    if (this.sisId.length() == 4 && other.sisId.length() == 6) {
      return -1;
    } else if (this.sisId.length() == 6 && other.sisId.length() == 4) {
      return 1;
    } else if (this.sisId.length() == 4 && other.sisId.length() == 4) {
      return other.sisId.compareTo(this.sisId);
    } else if (this.sisId.length() == 6 && other.sisId.length() == 6) {
      String thisTerm = this.sisId.substring(0, 4) + this.wiseSemesterToNumber();
      String otherTerm = other.sisId.substring(0, 4) + other.wiseSemesterToNumber();
      return otherTerm.compareTo(thisTerm);
    }
    return 0;
  }

  /**
   * Take the end part of a Wise term and turn it into a number so we can order the terms by most
   * recent
   *
   * @return the end part of a Wise term as a number
   */
  private String wiseSemesterToNumber() {
    if (this.sisId.length() != 6) {
      return "";
    }
    String semester = this.sisId.substring(4);
    return switch (semester) {
      case "S1" -> "1";
      case "S2" -> "2";
      case "FA" -> "3";
      case "SP" -> "4";
      default -> "";
    };
  }
}

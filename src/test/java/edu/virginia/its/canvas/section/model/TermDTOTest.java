package edu.virginia.its.canvas.section.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TermDTOTest {

  @Test
  void compareTo() {
    TermDTO cvilleFall24 = new TermDTO("1248", "cvilleFall24");
    TermDTO cvilleSummer23 = new TermDTO("1236", "cvilleSummer23");
    TermDTO cvilleSpring24 = new TermDTO("1242", "cvilleSpring24");
    TermDTO cvilleJterm24 = new TermDTO("1241", "cvilleJterm24");
    TermDTO wiseFall24 = new TermDTO("2425FA", "wiseFall24");
    TermDTO wiseSummerI24 = new TermDTO("2425S1", "wiseSummerI24");
    TermDTO wiseSummerII24 = new TermDTO("2425S2", "wiseSummerII24");
    TermDTO wiseSummerII23 = new TermDTO("2324S2", "wiseSummerII23");
    TermDTO wiseSpring24 = new TermDTO("2324SP", "wiseSpring24");
    List<TermDTO> terms =
        new ArrayList<>(
            List.of(
                wiseSummerII23,
                wiseSummerII24,
                wiseFall24,
                cvilleFall24,
                cvilleSummer23,
                wiseSummerI24,
                cvilleJterm24,
                wiseSpring24,
                cvilleSpring24));
    Collections.sort(terms);
    assertEquals(9, terms.size());
    assertEquals(cvilleFall24, terms.get(0));
    assertEquals(cvilleSpring24, terms.get(1));
    assertEquals(cvilleJterm24, terms.get(2));
    assertEquals(cvilleSummer23, terms.get(3));
    assertEquals(wiseFall24, terms.get(4));
    assertEquals(wiseSummerII24, terms.get(5));
    assertEquals(wiseSummerI24, terms.get(6));
    assertEquals(wiseSpring24, terms.get(7));
    assertEquals(wiseSummerII23, terms.get(8));
  }
}

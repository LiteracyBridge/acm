package org.literacybridge.acm.utils;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by bill on 10/4/16.
 */
public class B26RotatingEncodingTest {

  @Test
  public void testSmall() {
    // encode / decode one small number
    int i = 1;
    String s = B26RotatingEncoding.encode(i);
    int r = B26RotatingEncoding.decode(s);
    assertEquals("Numbers should be equal", i, r );
  }

  @Test
  public void testLarge() {
    // encode / decode one large number
    int i = 1000000;
    String s = B26RotatingEncoding.encode(i);
    int r = B26RotatingEncoding.decode(s);
    assertEquals("Numbers should be equal", i, r );
  }

  @Test
  public void test100k() {
    // encode / decode 100k numbers
    for (int i = 1; i<1000000; i+=10) {
      String s = B26RotatingEncoding.encode(i);
      int r = B26RotatingEncoding.decode(s);
      assertEquals("Numbers should be equal", i, r);
    }
  }

  @Test
  public void test1MUnique() {
    // encode / decode 1M distinct numbers, check for no duplicate strings
    Set<String> prevStrings = new HashSet<>();
    int count = 0;
    for (int i = 0; i<10000000; i+=10) {
      count++;
      String s = B26RotatingEncoding.encode(i);
      boolean seen = prevStrings.contains(s);
      assertFalse("Should not have already seen a string", seen);
      prevStrings.add(s);
      int r = B26RotatingEncoding.decode(s);
      assertEquals("Numbers should be equal", i, r);
    }
    assertEquals("Should have same # strings as # numbers", count, prevStrings.size());
  }

  @Test
  public void test4CharacterLength() {
    // 4 characters should encode 26^4 values. See if it does.
    int v = 26 * 26 * 26 * 26;
    String s = B26RotatingEncoding.encode(v-1);
    assertEquals("v should encode in 4 characters", 4, s.length());
    s = B26RotatingEncoding.encode(v);
    assertEquals("v should encode in 5 characters", 5, s.length());
  }

  @Test
  public void testNegative() {
    boolean exception = false;
    try {
      B26RotatingEncoding.encode(-1);
    } catch (Exception ignored) {
      exception = true;
    }
    assertTrue("Should have thrown exception with negative input", exception);
  }
}

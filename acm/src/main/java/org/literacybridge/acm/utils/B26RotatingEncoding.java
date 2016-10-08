package org.literacybridge.acm.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bill on 10/5/16.
 *
 * This class encapsulates converting integers to and from short character IDs.
 *
 * The idea is that we can assign a sequence number to a piece of user feedback,
 * then encode it to a short, but somewhat random appearing, string (think of
 * an airline confirmation code). Someone entering notes about the feedback can
 * easily write down a 4 to 6 letter string, but can't possibly transcribe a
 * message's real ID, not reliably.
 *
 * This is closely related to a base-26 encoding, except that each digit's symbols
 * is rotated by a value corresponding to the previous digit's symbol. Thus, adding
 * one to the value changes all of the digits, and so a sequence of consecutive
 * inputs generates a set of outputs where each is completely different from the
 * previous.
 *
 */
public class B26RotatingEncoding {
  private static final String OFFSET_MAP = "BCDFGHJKLMNPQRSTVWXYZ";
  private static final String SYMBOLS    = "LRXZMPCHWQYGTBNJKFSVD"; // entire alphabet, scrambled
  public static final int RADIX = SYMBOLS.length();
  public static final int MINIMUM_LENGTH = 4;

  /**
   * Given a positive integer, create a string that encodes the value.
   * @param v A positive integer.
   * @return The string that encodes the integer.
   */
  public static String encode(int v) {
    if (v < 0) {
      throw new IllegalArgumentException("Argument must not be negative");
    }
    StringBuilder encodedDigits = new StringBuilder();
    // convert the number to base-radix digits, least significant digit in digits[0]
    List<Integer> digits = new ArrayList<>();
    while (v > 0) {
      digits.add(v % RADIX);
      v = v / RADIX;
    }
    // pad out to the desired minimum length
    while (digits.size() < MINIMUM_LENGTH) {
      digits.add(0);
    }

    // starting offset is 0, will be different for every digit
    int offset = 0;
    for (int d : digits) {
      // look up the symbol for the digit, offset by 'offset'
      char ch = SYMBOLS.charAt((d + offset) % RADIX);
      // Add each new digit to the left of the string; moving least- to most-significant.
      encodedDigits.insert(0, ch);
      // for the next most significant digit's offset, use the just-generated encoded digit
      offset = OFFSET_MAP.indexOf(ch);
    }

    return encodedDigits.toString();
  }

  /**
   * Given a string produced by 'encode', return the original integer.
   * @param s A string produced by 'encode'.
   * @return The original integer.
   */
  public static int decode(String s) {
    List<Integer> digits = new ArrayList<>();
    // Convert input string to list of encoded digits, with least significant digit in encoded[0]
    char[] encodedDigits = new StringBuilder(s).reverse().toString().toCharArray();

    // starting offset is 0, will be different for every digit
    int offset = 0;
    for (char e : encodedDigits) {
      // look up the encoded digits; that index - offset is the original digit (mod radix, of course)
      int d = SYMBOLS.indexOf(e) - offset;
      if (d < 0) {
        d += RADIX;
      }
      digits.add(d);
      // for the next most significant digit's offset, use the place value of
      // the digit that was just decoded
      offset = OFFSET_MAP.indexOf(e);
    }

    // convert the array of digits back to a number
    Collections.reverse(digits);
    int result = 0;
    for (int d : digits) {
      result = (result * RADIX) + d;
    }
    return result;

  }


}

package org.literacybridge.core.tbloader;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TBLoaderUtils {
  public static String getDateTime() {
    SimpleDateFormat sdfDate = new SimpleDateFormat(
        "yyyy'y'MM'm'dd'd'HH'h'mm'm'ss's'");
    String dateTime = sdfDate.format(new Date());
    return dateTime;
  }
}

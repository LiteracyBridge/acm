package org.literacybridge.acm.utils;

public class OSChecker {
	  /** The value of <tt>System.getProperty("os.name")<tt>. **/
	  public static final String OS_NAME = System.getProperty("os.name");
	  /** True iff running on Linux. */
	  public static final boolean LINUX = OS_NAME.startsWith("Linux");
	  /** True iff running on Windows. */
	  public static final boolean WINDOWS = OS_NAME.startsWith("Windows");
	  /** True iff running on SunOS. */
	  public static final boolean SUN_OS = OS_NAME.startsWith("SunOS");
	  /** True iff running on SunOS. */
	  public static final boolean MAC_OS = OS_NAME.startsWith("Mac OS");
}

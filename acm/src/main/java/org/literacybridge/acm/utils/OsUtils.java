package org.literacybridge.acm.utils;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OsUtils {
  /** The value of <tt>System.getProperty("os.name")<tt>. **/
  public static final String OS_NAME = System.getProperty("os.name");
  /** True iff running on Linux. */
  public static final boolean LINUX = OS_NAME.startsWith("Linux");
  /** True iff running on Windows. */
  public static final boolean WINDOWS = OS_NAME.startsWith("Windows");
  /** True iff running on SunOS. */
  public static final boolean SUN_OS = OS_NAME.startsWith("SunOS");
  /** True iff running on MacOS. */
  public static final boolean MAC_OS = OS_NAME.startsWith("Mac OS");


  // Following MacOS specific functions taken from
  // http://stackoverflow.com/questions/7456227/how-to-handle-events-from-keyboard-and-mouse-in-full-screen-exclusive-mode-in-ja/30308671#30308671

  /**
   * If running on OSX, set the system property 'com.apple.eawt.QuitStrategy' to
   * 'CLOSE_ALL_WINDOWS'. This allows ⌘ + Q to properly close the application, by
   * closing windows before exiting.
   */
  public static void enableOSXQuitStrategy() {
    if (!MAC_OS) {
      return;
    }

    try {
      Class application = Class.forName("com.apple.eawt.Application");
      Method getApplication = application.getMethod("getApplication");
      Object instance = getApplication.invoke(application);
      Class strategy = Class.forName("com.apple.eawt.QuitStrategy");
      Enum closeAllWindows = Enum.valueOf(strategy, "CLOSE_ALL_WINDOWS");
      Method method = application.getMethod("setQuitStrategy", strategy);
      method.invoke(instance, closeAllWindows);
    } catch (ClassNotFoundException | NoSuchMethodException |
            SecurityException | IllegalAccessException |
            IllegalArgumentException | InvocationTargetException exp) {
      exp.printStackTrace(System.err);
    }
  }

  //FullScreenUtilities.setWindowCanFullScreen(window, true);

  /**
   * If running on OSX, allows the given window to enter full-screen mode, if the
   * user so chooses. (It replaces the maximize icon, a "+" sign, with full-screen
   * icon, somewhat like this ⤢; when clicked, the application is maximized to full
   * screen.)
   * @param window
   */
  public static void enableOSXFullscreen(Window window) {
    if (!MAC_OS) {
      return;
    }

    try {
      Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
      Class params[] = new Class[]{Window.class, Boolean.TYPE};
      Method method = util.getMethod("setWindowCanFullScreen", params);
      method.invoke(util, window, true);
    } catch (ClassNotFoundException | NoSuchMethodException |
            SecurityException | IllegalAccessException |
            IllegalArgumentException | InvocationTargetException exp) {
      exp.printStackTrace(System.err);
    }
  }

}

package org.literacybridge.acm.gui;

import java.net.URL;

public abstract class UIConstants {
  public static final String ICON_EDIT_16_PX = "edit16px.png";
  public static final String ICON_TRASH_16_PX = "trash16px.png";
  public static final String ICON_DELETE_16_PX = "delete16px.png";
  public static final String ICON_EXPORT_16_PX = "export16px.png";
  public static final String ICON_GRID_16_PX = "grid16px.png";
  public static final String ICON_SETTINGS_16_PX = "gear-16px.png";
  public static final String ICON_LANGUAGE_24_PX = "language-24px.png";

  public static final String ICON_BACKWARD = "back.png";
  public static final String ICON_STOP = "stop.png";
  public static final String ICON_PAUSE = "pause.png";
  public static final String ICON_PLAY = "play.png";
  public static final String ICON_FORWARD = "forward.png";
    public static final String ICON_GEAR_32_PX = "gears-32.png";
    public static final String ICON_FILTER = "filter-96.png";
    public static final String ICON_UNPINNED = "unpinned-96.png";
    public static final String ICON_PINNED = "pinned-96.png";
  public static final String ICON_ASSISTANT_32_PX = "assistant-32b.png";
  public static final String ICON_SEARCH_32_PX = "search-glass-32px.png";

  public static final String SPLASH_SCREEN_IMAGE = "splash-acm.jpg";
  public static final String GEARS_64_PNG = "gears_64.png";
  public static final String TREE_64_PNG = "tree_64.png";
  public static final String SHORTCUTS_64_PNG = "shortcuts_64.png";
  public static final String TEST_64_PNG = "test_64.png";

  public static URL getResource(String resourceName) {
    return UIConstants.class.getResource("/" + resourceName);
  }

}

package org.literacybridge.acm.gui.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.dialogs.BusyDialog;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Playlist;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UIUtils {
  public static <T extends Container> T showDialog(Window parent, final T dialog) {
    final Dimension frameSize = parent!=null?parent.getSize():new Dimension(0,0);
    final int x = (frameSize.width - dialog.getWidth()) / 2;
    final int y = (frameSize.height - dialog.getHeight()) / 2;
    return showDialog(dialog, x, y);
  }

    public static <T extends Container> T showDialog(final T dialog, int ox, int oy) {
        // If dialog extends past the bottom or right, move it back to fit. Don't go off top or left.

        Rectangle deviceBounds = dialog.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
        int dx = dialog.getWidth();
        int x = Math.min(ox, deviceBounds.x + deviceBounds.width - dx - 10);
        int dy = dialog.getHeight();
        int y = Math.min(oy, deviceBounds.y + deviceBounds.height - dy - 20);

        // Uncomment to debug.
//      System.out.printf("show dlg, bounds:(%d,%d, %dx%d), at:(%d,%d)->(%d,%d)\n",
//              deviceBounds.x,
//              deviceBounds.y,
//              deviceBounds.width,
//              deviceBounds.height,
//              ox, oy, x,
//              y);

        dialog.setLocation(x, y);
        setVisible(dialog, true);
        return dialog;
    }

  /**
   * Runs some code with a wait spinner, with a callback when the work is done.
   * @param waitParent The parent window for the spinner.
   * @param runnable The code with the work to be done.
   * @param onFinished Called when the work has finished.
   * @param waitUiOptions Options for the placement of the spinner window.
   */
  public static void runWithWaitSpinner(String text, Window waitParent, Runnable runnable, Runnable onFinished, UiOptions... waitUiOptions) {
    final Runnable job = new Runnable() {
      BusyDialog dialog;

      @Override
      public void run() {
        Application app = Application.getApplication();
        dialog = UIUtils.showDialog(waitParent,
            new BusyDialog(LabelProvider.getLabel(text), waitParent));
        UIUtils.centerWindow(dialog, waitUiOptions);
        try {
          runnable.run();
        } finally {
          UIUtils.hideDialog(dialog);
          SwingUtilities.invokeLater(onFinished);
        }
      }
    };
    new Thread(job).start();
  }

  public enum UiOptions { TOP_THIRD, SHIFT_DOWN, HORIZONTAL_ONLY }
  public static <T extends Container> T centerWindow(final T window, UiOptions... optionFlags) {
    Set<UiOptions> options = new HashSet<>(Arrays.asList(optionFlags));
    // Center horizontally and in the top half or 2/3 of screen.
    Rectangle deviceBounds = window.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
    int x = Math.max(0, (int)(deviceBounds.x + deviceBounds.getWidth()/2 - window.getWidth()/2));
    int y;
    if (options.contains(UiOptions.HORIZONTAL_ONLY)) {
        y = window.getY();
    } else {
        int yDivisor = options.contains(UiOptions.TOP_THIRD) ? 3 : 2;
        y = Math.max(0, (int)(deviceBounds.y + deviceBounds.getHeight()/yDivisor - window.getHeight()/2));
        if (options.contains(UiOptions.SHIFT_DOWN)) {
            y += 30;
        }
    }

    window.setLocation(x, y);

    return window;
  }

  public static void hideDialog(final Container dialog) {
    setVisible(dialog, false);
  }

  public static void setVisible(final Container dialog, final boolean visible) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> dialog.setVisible(visible));
    } else {
      dialog.setVisible(visible);
    }
  }

  /**
   * Helper to update the text of a label on the proper thread.
   * @param label The label to be updated.
   * @param text The text with which to be updated.
   */
  public static void setLabelText(final JLabel label, final String text) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> label.setText(text));
    } else {
      label.setText(text);
      try {Thread.sleep(100);} catch(Exception ignored) {}
    }
  }

  public static void setProgressBarValue(final JProgressBar progressBar, final int value) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> progressBar.setValue(value));
    } else {
      progressBar.setValue(value);
    }
  }


  public static void appendLabelText(final JTextComponent textComponent,
      final String newText,
      final boolean prepend)
  {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> doAppendText(textComponent, newText, prepend));
    } else {
      doAppendText(textComponent, newText, prepend);
    }
  }
  private static void doAppendText(final JTextComponent textComponent,
      final String newText,
      final boolean prepend)
  {
    String oldText = textComponent.getText();
    if (StringUtils.isNotEmpty(oldText)) oldText = prepend ? "\n"+oldText : oldText+"\n";
    oldText = prepend ? newText+oldText : oldText+newText;
    textComponent.setText(oldText);
  }

  public static void invokeAndWait(final Runnable runnable) {
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        runnable.run();
      } else {
        SwingUtilities.invokeAndWait(runnable);
      }
    } catch (InvocationTargetException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getCategoryNamesAsString(AudioItem audioItem) {
      return getCategoryNamesAsString(audioItem, false);
  }

  public static String getCategoryNamesAsString(AudioItem audioItem, boolean fullName) {
    StringBuilder builder = new StringBuilder();

    for (Category cat : audioItem.getCategoryList()) {
      if (!cat.hasChildren()) {
        if (builder.length() > 0) {
          builder.append(", ");
        }
        builder.append(fullName ? cat.getFullName() : cat.getCategoryName());
      }
    }
    return builder.toString();
  }

    public static String getCategoryCodesAsString(AudioItem audioItem) {
        StringBuilder builder = new StringBuilder();

        for (Category cat : audioItem.getCategoryList()) {
            if (!cat.hasChildren()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(cat.getId());
            }
        }
        return builder.toString();
    }

    public static String getPlaylistAsString(AudioItem audioItem) {
    List<Playlist> playlists = Lists.newArrayList(audioItem.getPlaylists());
    StringBuilder builder = new StringBuilder();

    int i = 0;
    for (Playlist playlist : playlists) {
      builder.append(playlist.getName());
      if (i != playlists.size() - 1) {
        builder.append(", ");
      }
      i++;
    }
    return builder.toString();
  }

}

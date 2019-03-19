package org.literacybridge.acm.gui.util;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Playlist;

import com.google.common.collect.Lists;

public class UIUtils {
  public static <T extends Container> T showDialog(Frame parent, final T dialog) {
    final Dimension frameSize = parent.getSize();
    final int x = (frameSize.width - dialog.getWidth()) / 2;
    final int y = (frameSize.height - dialog.getHeight()) / 2;
    return showDialog(dialog, x, y);
  }

  public static <T extends Container> T showDialog(final T dialog, int x, int y) {

    dialog.setLocation(x, y);
    setVisible(dialog, true);
    return dialog;
  }

  public static void hideDialog(final Container dialog) {
    setVisible(dialog, false);
  }

  private static void setVisible(final Container dialog, final boolean visible) {
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
    }
  }

  public static void appendLabelText(final JTextComponent textComponent,
      final String newText,
      final boolean prepend)
  {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> {
        doAppendText(textComponent, newText, prepend);
      });
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

package org.literacybridge.acm.gui.util;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import javax.swing.SwingUtilities;

import org.literacybridge.acm.categories.Taxonomy.Category;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.gui.util.language.LanguageUtil;

public class UIUtils {
	public static Container showDialog(Frame parent, final Container dialog) {
		final Dimension frameSize = parent.getSize();
		final int x = (frameSize.width - dialog.getWidth()) / 2;
		final int y = (frameSize.height - dialog.getHeight()) / 2;
		return showDialog(dialog, x, y);
	}

	public static Container showDialog(final Container dialog, int x,
			int y) {

		dialog.setLocation(x, y);
		setVisible(dialog, true);
		return dialog;
	}
	
	public static void hideDialog(final Container dialog) {
		setVisible(dialog, false);
	}

	
	private static void setVisible(final Container dialog, final boolean visible) {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					dialog.setVisible(visible);
				}
			});
		} else {
			dialog.setVisible(visible);
		}
	}

	public static void invokeAndWait(final Runnable runnable) {
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				runnable.run();
			} else {
				SwingUtilities.invokeAndWait(runnable);
			}
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getCategoryListAsString(AudioItem audioItem) {
		List<Category> categories = audioItem.getCategoryList();
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < categories.size(); i++) {
			Category cat = categories.get(i);
			if (!cat.hasChildren()) {
				if (builder.length() > 0) {
					builder.append(", ");
				}
				builder.append(cat.getCategoryName(LanguageUtil.getUILanguage()));
			}
		}
		return builder.toString();
	}
	
	public static String getPlaylistAsString(AudioItem audioItem) {
		Collection<PersistentTag> playlists = audioItem.getPlaylists();
		StringBuilder builder = new StringBuilder();
		
		int i = 0;
		for (PersistentTag playlist : playlists) {
			builder.append(playlist.getName());
			if (i != playlists.size() - 1) {
				builder.append(", ");
			}
			i++;
		}
		return builder.toString();
	}

}

package org.literacybridge.acm.gui.ResourceView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.db.PersistentAudioItem;
import org.literacybridge.acm.db.PersistentTag;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.ResourceView.CategoryView.TagsListChanged;
import org.literacybridge.acm.gui.ResourceView.TagsListModel.TagLabel;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.language.LanguageUtil;

public class TagsListPopupMenu extends JPopupMenu {
	private static final Logger LOG = Logger.getLogger(TagsListPopupMenu.class.getName());
	
	public TagsListPopupMenu(final TagLabel selectedTag) {
		JMenuItem deleteTag = new JMenuItem("Delete '" + selectedTag + "' ...");
		JMenuItem renameTag = new JMenuItem("Rename '" + selectedTag + "' ...");
		
		add(deleteTag);
		add(renameTag);
		
		deleteTag.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				Object[] options = {LabelProvider.getLabel("CANCEL", LanguageUtil.getUILanguage())
						, LabelProvider.getLabel("DELETE", LanguageUtil.getUILanguage())};
				
				int n = JOptionPane.showOptionDialog(Application.getApplication(),
					    "Delete playlist '" + selectedTag + "'?",
					    LabelProvider.getLabel("CONFRIM_DELETE", LanguageUtil.getUILanguage()),
					    JOptionPane.OK_CANCEL_OPTION,
					    JOptionPane.QUESTION_MESSAGE,
					    null,
					    options,
					    options[0]);

				if (n == 1) {
					try {
						List<PersistentAudioItem> audioItems = selectedTag.getTag().getPersistentAudioItemList();
						for (PersistentAudioItem audioItem : audioItems) {
							audioItem.removePersistentTag(selectedTag.getTag());
							audioItem.commit();
						}
						selectedTag.getTag().destroy();
						Application.getMessageService().pumpMessage(new TagsListChanged(PersistentTag.getFromDatabase()));
					} catch (Exception ex) {
						LOG.log(Level.WARNING, "Unable to remove playlist " + selectedTag.toString());
					} finally {
						Application.getFilterState().updateResult(true);
					}
				}
			}
		});
		
		renameTag.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				String tagName = (String)JOptionPane.showInputDialog(
						TagsListPopupMenu.this,
		                "Enter playlist name:",
		                "Edit playlist",
		                JOptionPane.PLAIN_MESSAGE,
		                null, null, selectedTag.toString());
				if (!StringUtils.isEmpty(tagName)) {
					try {
						selectedTag.getTag().setTitle(tagName);
						selectedTag.getTag().commit();
						
						Application.getMessageService().pumpMessage(new TagsListChanged(PersistentTag.getFromDatabase()));
					} catch (Exception ex) {
						LOG.log(Level.WARNING, "Unable to rename playlist " + selectedTag.toString());
					} finally {
						Application.getFilterState().updateResult(true);
					}						
				}
			}
		});
	}
}

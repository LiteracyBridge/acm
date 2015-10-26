package org.literacybridge.acm.gui.ResourceView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.db.Taxonomy;
import org.literacybridge.acm.db.Taxonomy.Category;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.ResourceView.CategoryView.TagsListChanged;
import org.literacybridge.acm.gui.ResourceView.TagsListModel.TagLabel;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.language.LanguageUtil;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TagsListPopupMenu extends JPopupMenu {
    private static final Logger LOG = Logger.getLogger(TagsListPopupMenu.class
            .getName());

    private static String previousPackageName = "";

    public TagsListPopupMenu(final TagLabel selectedTag) {
        JMenuItem deleteTag = new JMenuItem("Delete '" + selectedTag + "' ...");
        JMenuItem renameTag = new JMenuItem("Rename '" + selectedTag + "' ...");
        JMenuItem exportTag = new JMenuItem("Export '" + selectedTag + "' ...");

        add(deleteTag);
        add(renameTag);
        add(exportTag);

        deleteTag.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] options = {
                        LabelProvider.getLabel("CANCEL",
                                LanguageUtil.getUILanguage()),
                        LabelProvider.getLabel("DELETE",
                                LanguageUtil.getUILanguage()) };

                int n = JOptionPane.showOptionDialog(
                        Application.getApplication(),
                        "Delete playlist '" + selectedTag + "'?",
                        LabelProvider.getLabel("CONFRIM_DELETE",
                                LanguageUtil.getUILanguage()),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (n == 1) {
                    try {
                        List<AudioItem> audioItems = Lists.newLinkedList(selectedTag
                                .getTag().getAudioItemList());
                        for (AudioItem audioItem : audioItems) {
                            audioItem.removePlaylist(selectedTag.getTag());
                            audioItem.commit();
                        }
                        selectedTag.getTag().destroy();
                        Application.getMessageService().pumpMessage(
                                new TagsListChanged(
                                        ACMConfiguration.getCurrentDB().getMetadataStore().getPlaylists()));
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "Unable to remove playlist "
                                + selectedTag.toString());
                    } finally {
                        Application.getFilterState().updateResult(true);
                    }
                }
            }
        });

        renameTag.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String tagName = (String) JOptionPane.showInputDialog(
                        TagsListPopupMenu.this, "Enter playlist name:",
                        "Edit playlist", JOptionPane.PLAIN_MESSAGE, null, null,
                        selectedTag.toString());
                if (!StringUtils.isEmpty(tagName)) {
                    try {
                        selectedTag.getTag().setName(tagName);
                        selectedTag.getTag().commit();

                        Application.getMessageService().pumpMessage(
                                new TagsListChanged(
                                        ACMConfiguration.getCurrentDB().getMetadataStore().getPlaylists()));
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "Unable to rename playlist "
                                + selectedTag.toString());
                    } finally {
                        Application.getFilterState().updateResult(true);
                    }
                }
            }
        });

        exportTag.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                File listDirectory = new File(ACMConfiguration.getCurrentDB()
                        .getTBLoadersDirectory(),
                        "TB_Options/activeLists");
                LinkedHashMap<String, Category> categories = new LinkedHashMap();
                Map<String, File> listCollection = Maps.newHashMap();
                try {
                    String packageName = (String) JOptionPane.showInputDialog(
                            Application.getApplication(), "Enter content package name:",
                            "Export playlist", JOptionPane.PLAIN_MESSAGE, null,
                            null, previousPackageName);

                    if (!StringUtils.isEmpty(packageName)) {
                        previousPackageName = packageName;
                        //TODO: need to accommodate multiple message lists (or profiles) in a single package/image
                        //TODO: each new message list would be numbered
                        File dir = new File(ACMConfiguration.getCurrentDB().getTBLoadersDirectory(), "packages/"
                                + packageName + "/messages/lists/" + TBBuilder.firstMessageListName);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        File targetActiveListsFile;
                        targetActiveListsFile = new File(dir, "_activeLists.txt");
                        if (!targetActiveListsFile.exists()) {
                            File sourceActiveListsFile;
                            if (listDirectory.listFiles().length > 1) {
                                for (File possibleActiveListFile : listDirectory.listFiles()) {
                                    String possibleActiveListString = possibleActiveListFile.getName();
                                    possibleActiveListString = possibleActiveListString.substring(0, possibleActiveListString.length()-4);
                                    possibleActiveListString = possibleActiveListString.replace('_', ' ');
                                    listCollection.put(possibleActiveListString, possibleActiveListFile);
                                }
                                String[] listNames = listCollection.keySet().toArray(
                                        new String[listCollection.size()]);
                                String listName = (String) JOptionPane
                                        .showInputDialog(Application.getApplication(),
                                                "Choose categories & order:",
                                                "Category Order",
                                                JOptionPane.PLAIN_MESSAGE, null, listNames,
                                                "");
                                if (!StringUtils.isEmpty(listName)) {
                                    sourceActiveListsFile = listCollection.get(listName);
                                } else {
                                    sourceActiveListsFile = listDirectory.listFiles()[0];
                                }
                            } else {
                                sourceActiveListsFile = listDirectory.listFiles()[0];
                            }
                            IOUtils.copy(sourceActiveListsFile, targetActiveListsFile);
                        }

                        Category category = Taxonomy
                                .getFromDatabase(TBBuilder.IntroMessageID); // Update Intro Message
                        categories.put(category.getCategoryName(LanguageUtil.getUILanguage()), category);
                        BufferedReader reader = new BufferedReader(new FileReader(
                                targetActiveListsFile));
                        while (reader.ready()) {
                            String line = reader.readLine();
                            if (StringUtils.isEmpty(line)) {
                                break;
                            }
                            if (line.contains("$")) {
                                continue;
                            }
                            if (line.startsWith("!")) {
                                line = line.substring(1);
                            }

                            category = Taxonomy.getFromDatabase(line);
                            if (category != null) {
                                categories.put(category.getCategoryName(LanguageUtil.getUILanguage()),
                                        category);
                            }
                        }

                        reader.close();


                        String[] names = categories.keySet().toArray(
                                new String[categories.size()]);
                        String categoryName = (String) JOptionPane
                                .showInputDialog(Application.getApplication(),
                                        "Choose export category:",
                                        "Export playlist",
                                        JOptionPane.PLAIN_MESSAGE, null, names,
                                        "");

                        if (!StringUtils.isEmpty(categoryName)) {
                            export(selectedTag.getTag(), packageName,
                                    categories.get(categoryName), dir);
                        }
                    }

                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Error while exporting playlist.", e);
                }
            }
        });
    }

    private final void export(final Playlist playlist, String updateName,
            Category category, File dir) throws IOException {

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir,
                category.getUuid() + ".txt"), false));
        List<AudioItem> audioItems = playlist.getAudioItemList();
        Collections.sort(audioItems, new Comparator<AudioItem>() {
            @Override
            public int compare(AudioItem a1, AudioItem a2) {
                return Integer.compare(playlist.getPosition(a1),
                        playlist.getPosition(a2));
            }
        });

        for (AudioItem audioItem : audioItems) {
            writer.write(audioItem.getUuid());
            writer.newLine();
        }

        writer.close();
    }
}

package org.literacybridge.acm.gui.MainWindow;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.SidebarView.PlaylistsChanged;
import org.literacybridge.acm.gui.MainWindow.PlaylistListModel.PlaylistLabel;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Playlists are still called "Tags". This is the context menu for playlists.
 */
class PlaylistPopupMenu extends JPopupMenu {
    private static final Logger LOG = Logger
        .getLogger(PlaylistPopupMenu.class.getName());

    private static String previousPackageName = "";
    private final PlaylistLabel selectedPlaylist;

    PlaylistPopupMenu(final PlaylistLabel selectedPlaylist) {
        this.selectedPlaylist = selectedPlaylist;
        String playlist = selectedPlaylist.getPlaylist().getName();
        JMenuItem deletePlaylist = new JMenuItem("Delete '" + playlist + "' ...");
        JMenuItem renamePlaylist = new JMenuItem("Rename '" + playlist + "' ...");
        JMenuItem exportPlaylist = new JMenuItem("Export '" + playlist + "' ...");

        add(deletePlaylist);
        add(renamePlaylist);
        add(exportPlaylist);

        deletePlaylist.addActionListener(deleteListener);
        renamePlaylist.addActionListener(renameListener);
        exportPlaylist.addActionListener(exportListener);
    }

    /**
     * Called when the delete menu item is clicked.
     */
    private ActionListener deleteListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object[] options = {
                LabelProvider.getLabel("CANCEL"),
                LabelProvider.getLabel("DELETE") };

            int n = JOptionPane.showOptionDialog(Application.getApplication(),
                "Delete playlist '" + selectedPlaylist + "'?",
                LabelProvider.getLabel("CONFRIM_DELETE"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]);

            if (n == 1) {
                try {
                    List<String> audioItems = Lists
                        .newLinkedList(selectedPlaylist.getPlaylist().getAudioItemList());
                    for (String audioItemUuid : audioItems) {
                        AudioItem audioItem = ACMConfiguration.getInstance()
                            .getCurrentDB().getMetadataStore()
                            .getAudioItem(audioItemUuid);
                        audioItem.removePlaylist(selectedPlaylist.getPlaylist());
                        ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
                            .commit(audioItem);
                    }
                    ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
                        .deletePlaylist(selectedPlaylist.getPlaylist().getUuid());
                    ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
                        .commit(selectedPlaylist.getPlaylist());
                    Application.getMessageService().pumpMessage(new PlaylistsChanged());
                } catch (Exception ex) {
                    LOG.log(Level.WARNING,
                        "Unable to remove playlist " + selectedPlaylist.toString());
                } finally {
                    Application.getFilterState().updateResult(true);
                }
            }
        }
    };

    /**
     * Called when the rename menu item is clicked.
     */
    private ActionListener renameListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            String playlistName = (String) JOptionPane.showInputDialog(
                PlaylistPopupMenu.this, "Enter playlist name:", "Edit playlist",
                JOptionPane.PLAIN_MESSAGE, null, null, selectedPlaylist.getPlaylist().getName());
            if (!StringUtils.isEmpty(playlistName)) {
                try {
                    selectedPlaylist.getPlaylist().setName(playlistName);
                    ACMConfiguration.getInstance().getCurrentDB().getMetadataStore()
                        .commit(selectedPlaylist.getPlaylist());

                    Application.getMessageService().pumpMessage(new PlaylistsChanged());
                } catch (Exception ex) {
                    LOG.log(Level.WARNING,
                        "Unable to rename playlist " + selectedPlaylist.toString());
                } finally {
                    Application.getFilterState().updateResult(true);
                }
            }
        }
    };

    /**
     * Called when the export menu item is clicked.
     */
    private ActionListener exportListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent event) {
            LinkedHashMap<String, Category> categories = new LinkedHashMap<>();
            try {
                // Prompt the user for the name of the exported package. If we've exported a
                // package previously in this invocation of the ACM, use that package name as
                // an initial value; user can leave that alone to continue exporting playlists
                // into the package, or can enter a new name, to start a new package.
                String packageName = promptWithMaxLength("Export Playlist",
                    "Enter Content Package name:", MAX_PACKAGE_NAME_LENGTH, previousPackageName);

                if (!StringUtils.isEmpty(packageName)) {
                    // If the user actually entered anything, create the directory for the package.
                    previousPackageName = packageName;
                    File packageMessagesListsDir = new File(
                        ACMConfiguration.getInstance().getCurrentDB()
                            .getTBLoadersDirectory(),
                        "packages/" + packageName + "/messages/lists/"
                            + TBBuilder.firstMessageListName);
                    if (!packageMessagesListsDir.exists()) {
                        packageMessagesListsDir.mkdirs();
                    }

                    // If this is the first time that a playlist has been exported into this package,
                    // there will be no _activeLists.txt file. In that case, prompt the user for
                    // which _activeLists.txt they want. The files reside in the ACM directory, in
                    // TB-Loaders / TB_Options / activeLists.
                    // The contents of these files must be actual, valid _activeLists.txt files.
                    // By convention, the files are named as "Health_Farming_....txt", where
                    // the words are suggestive of the categories, in order, in the file.
                    File targetActiveListsFile;
                    targetActiveListsFile = new File(packageMessagesListsDir, "_activeLists.txt");
                    if (!targetActiveListsFile.exists()) {
                        File sourceActiveListsFile = getActiveListsForExport();
                        IOUtils.copy(sourceActiveListsFile, targetActiveListsFile);
                    }

                    MetadataStore store = ACMConfiguration.getInstance().getCurrentDB()
                        .getMetadataStore();

                    // Add "Intro Message" to list of categories. hard coded as "0-5". "Intro Message"
                    Category category = store.getCategory(TBBuilder.IntroMessageID);
                    categories.put(category.getCategoryName(), category);

                    // Read the categories from the _activeLists.txt file.
                    BufferedReader reader = new BufferedReader(new FileReader(targetActiveListsFile));
                    while (reader.ready()) {
                        String line = reader.readLine();
                        if (StringUtils.isEmpty(line)) {
                            break;
                        }
                        if (line.contains("$")) {
                            // Hacky way to skip "$0-1", with is the Talking Book description. It is
                            // built in, and pre-existing.
                            continue;
                        }
                        if (line.startsWith("!")) {
                            // The "!" means locked. Category follows the "!"
                            line = line.substring(1);
                        }

                        category = store.getCategory(line);
                        if (category != null) {
                            categories.put(category.getCategoryName(), category);
                        }
                    }
                    reader.close();

                    // Prompt the user for the Category name / Playlist name for this playlist. If they
                    // choose a name that's already been exported to, the existing file will be silently
                    // overwritten.
                    String[] names = categories.keySet().toArray(new String[categories.size()]);
                    String categoryName = (String) JOptionPane.showInputDialog(
                        Application.getApplication(), "Choose export category:",
                        "Export playlist", JOptionPane.PLAIN_MESSAGE, null, names, "");

                    if (!StringUtils.isEmpty(categoryName)) {
                        export(selectedPlaylist.getPlaylist(), categories.get(categoryName), packageMessagesListsDir);
                    }
                }

            } catch (IOException e) {
                LOG.log(Level.WARNING, "Error while exporting playlist.", e);
            }
        }
    };

    /**
     * Exports a playlist, by creating a file with a list of the message ids in the playlist.
     * <p>
     * The file is created as:
     * ~/Dropbox/ACM-NAME/TB-Loaders/packages/PACKAGE_NAME/messages/lists/1/PLAYLIST_CATEGORY_NAME.txt
     *
     * @param playlist to be exported.
     * @param category The category code that is being used as the Playlist name. Note that this
     *                 has nothing whatsoever to do with the category of any message in the playlist.
     *                 We use the same list of "category names" to categorized messages, and to name
     *                 playlists.
     * @param dir      The directory into which the list of message ids will be written.
     * @throws IOException if the file can't be written.
     */
    private void export(
        final Playlist playlist, Category category, File dir) throws IOException {

        BufferedWriter writer = new BufferedWriter(
            new FileWriter(new File(dir, category.getId() + ".txt"), false));
        Iterable<String> audioItems = playlist.getAudioItemList();

        for (String audioItem : audioItems) {
            writer.write(audioItem);
            writer.newLine();
        }

        writer.close();
    }

    /**
     * Gets a template _activeLists.txt file. There may be several to choose from, in which case
     * the user is prompted. If only one file, it is returned directly.
     *
     * The templates are the files in
     * ~/Dropbox/ACM-NAME/TB-Loaders/TB_Options/activeLists
     *
     * @return the _activeLists.txt template file.
     */
    private File getActiveListsForExport() {
        File listDirectory = new File(ACMConfiguration.getInstance()
            .getCurrentDB().getTBLoadersDirectory(), "TB_Options/activeLists");
        File sourceActiveListsFile;
        Map<String, File> listCollection = Maps.newHashMap();
        if (listDirectory.listFiles().length > 1) {
            for (File possibleActiveListFile : listDirectory.listFiles()) {
                String possibleActiveListString = possibleActiveListFile.getName();
                // Remove ".txt" from the end.
                possibleActiveListString = possibleActiveListString
                    .substring(0, possibleActiveListString.length() - 4);
                // Convert from file-name-friendly underscores to human friendly spaces.
                possibleActiveListString = possibleActiveListString
                    .replace('_', ' ');
                // Add to list for user to choose from.
                listCollection.put(possibleActiveListString, possibleActiveListFile);
            }

            String[] listNames = listCollection.keySet().toArray(new String[listCollection.size()]);
            String listName = (String) JOptionPane.showInputDialog(
                Application.getApplication(), "Choose categories & order:",
                "Category Order", JOptionPane.PLAIN_MESSAGE, null,
                listNames, "");

            if (!StringUtils.isEmpty(listName)) {
                sourceActiveListsFile = listCollection.get(listName);
            } else {
                // Nothing chosen, so take first from list
                sourceActiveListsFile = listDirectory.listFiles()[0];
            }
        } else {
            // Only one file, so use it
            sourceActiveListsFile = listDirectory.listFiles()[0];
        }
        return sourceActiveListsFile;
    }


    /**
     * Prompts for a string, with a maximum length.
     */
    private static final int MAX_PACKAGE_NAME_LENGTH = 20;
    private JLabel charsRemainingPrompt;

    public String promptWithMaxLength(
        String title, String prompt, int maxLen, String initialValue) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(new JLabel(prompt));

        JTextField field = new JTextField(10);
        field.setText(initialValue);
        DocumentSizeFilter filter = new DocumentSizeFilter(maxLen);
        ((AbstractDocument) field.getDocument()).setDocumentFilter(filter);

        panel.add(field);

        charsRemainingPrompt = new JLabel();
        charsRemainingPrompt.setFont(new Font("Sans Serif", Font.ITALIC, 10));
        filter.setPrompt(null);
        panel.add(charsRemainingPrompt);

        field.addAncestorListener( new RequestFocusListener() );

        int result = JOptionPane.showOptionDialog(Application.getApplication(), panel, title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE, null, null, null);

        if (result == 0) {
            return field.getText();
        }
        return null;
    }

    private class DocumentSizeFilter extends DocumentFilter {
        int maxCharacters;
        boolean DEBUG = false;

        DocumentSizeFilter(int maxChars) {
            maxCharacters = maxChars;
        }

        private void setPrompt(FilterBypass fb) {
            int curCharacters = fb == null ? 0 : fb.getDocument().getLength();
            int remaining = maxCharacters - curCharacters;
            String prompt;
            if (remaining == 0)
                prompt = "No characters remaining.";
            else if (remaining == 1)
                prompt = "1 character remaining.";
            else
                prompt = String.format("%d characters remaining.", remaining);
            charsRemainingPrompt.setText(prompt);
        }

        public void insertString(
            FilterBypass fb, int offs,
            String str, AttributeSet a)
            throws BadLocationException {
            if (DEBUG) {
                System.out.println("in DocumentSizeFilter's insertString method");
            }

            //This rejects the entire insertion if it would make
            //the contents too long. Another option would be
            //to truncate the inserted string so the contents
            //would be exactly maxCharacters in length.
            if ((fb.getDocument().getLength() + str.length()) <= maxCharacters) {
                super.insertString(fb, offs, str, a);
                setPrompt(fb);
            } else
                Toolkit.getDefaultToolkit().beep();
        }

        public void replace(
            FilterBypass fb, int offs,
            int length,
            String str, AttributeSet a)
            throws BadLocationException {
            if (DEBUG) {
                System.out.println("in DocumentSizeFilter's replace method");
            }
            //This rejects the entire replacement if it would make
            //the contents too long. Another option would be
            //to truncate the replacement string so the contents
            //would be exactly maxCharacters in length.
            if ((fb.getDocument().getLength() + str.length() - length) <= maxCharacters) {
                super.replace(fb, offs, length, str, a);
                setPrompt(fb);
            } else
                Toolkit.getDefaultToolkit().beep();
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
            super.remove(fb, offset, length);
            setPrompt(fb);
        }
    }

    /**
     *  Convenience class to request focus on a component.
     *
     *  When the component is added to a realized Window then component will
     *  request focus immediately, since the ancestorAdded event is fired
     *  immediately.
     *
     *  When the component is added to a non realized Window, then the focus
     *  request will be made once the window is realized, since the
     *  ancestorAdded event will not be fired until then.
     *
     *  Using the default constructor will cause the listener to be removed
     *  from the component once the AncestorEvent is generated. A second constructor
     *  allows you to specify a boolean value of false to prevent the
     *  AncestorListener from being removed when the event is generated. This will
     *  allow you to reuse the listener each time the event is generated.
     */
    public class RequestFocusListener implements AncestorListener
    {
        private boolean removeListener;

        /*
         *  Convenience constructor. The listener is only used once and then it is
         *  removed from the component.
         */
        public RequestFocusListener()
        {
            this(true);
        }

        /*
         *  Constructor that controls whether this listen can be used once or
         *  multiple times.
         *
         *  @param removeListener when true this listener is only invoked once
         *                        otherwise it can be invoked multiple times.
         */
        public RequestFocusListener(boolean removeListener)
        {
            this.removeListener = removeListener;
        }

        @Override
        public void ancestorAdded(AncestorEvent e)
        {
            JComponent component = e.getComponent();
            component.requestFocusInWindow();

            if (removeListener)
                component.removeAncestorListener( this );
        }

        @Override
        public void ancestorMoved(AncestorEvent e) {}

        @Override
        public void ancestorRemoved(AncestorEvent e) {}
    }
}

package org.literacybridge.acm.gui.dialogs;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.acm.store.Transaction;
import org.literacybridge.core.spec.ContentSpec;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_UP;

/**
 * Dialog to rename multiple Audio Items. There are two ways that it can operate:
 * 1) Multiple audio items passed in, to receive a new name. User will be prompted
 *    to enter a name, based on the first item passed.
 * 2) One item passed in. Search for any other Audio Items that have the same name,
 *    and rename them all together.
 */
public class AudioItemRenameDialog extends JDialog {
    private Color dialogBackground = new Color(236,236,236);

    private JLabel renamePrompt;
    private JCheckBox manyToOneOk;
    private JCheckBox renameMatchingOk;
    private JTextField titleEdit;
    private JButton okButton;

    private boolean manyToOne = false;
    private boolean renameMatching = false;
    private String oldTitle;
    private List<AudioItem> renameList;

    private boolean haveProgramSpec = false;
    private List<String> specTitles = new ArrayList<>();
    private Map<String, List<ContentSpec.MessageSpec>> messageSpecs = new LinkedHashMap<>();

    // Popup
    private Window popupWindow;
    private JScrollPane popupScroller;
    private JList<String> suggestedTitles;

    private Map<String, BiFunction<String, String, Integer>> matchers;
    private Map<String, Boolean> matchersEnabled;
    private boolean adjustingSelection;
    private JCheckBox addToPlaylist;
    private JList<String> playlistList;
    private JCheckBox removeFromPlaylist;

    public AudioItemRenameDialog(final JFrame parent, AudioItem... audioItems) {
        super(parent, "Rename Audio Item", ModalityType.APPLICATION_MODAL);

        loadProgramSpec();

        if (audioItems.length < 1) return;
        oldTitle = audioItems[0].getTitle();

        List<AudioItem> audioItemsList = Arrays.asList(audioItems);
        if (audioItemsList.size() == 1) {
            renameList = findSameNamedItems(audioItems[0]);
            renameMatching = renameList.size()>1;
        } else {
            renameList = audioItemsList;
            manyToOne = true;
        }

        setLayout(new BorderLayout());
        add(layoutComponents(), BorderLayout.CENTER);
        add(layoutButtons(), BorderLayout.SOUTH);

        setRenamePrompt();
        enableOkButton();

        setResizable(true);
        Dimension d = getPreferredSize();
        d.width = Math.max(d.width, 500);
        d.height = Math.max(d.height, 400);
        setSize(d);

        addComponentListener(titleComponentListener);

        // Prepare the popup.
        suggestedTitles = new JList<>();
        suggestedTitles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestedTitles.setModel(suggestedTitlesModel);
        suggestedTitles.addListSelectionListener(titleSelectionListener);
        Font cf = LabelButton.fontResource(LabelButton.AVENIR);
        suggestedTitles.setFont(cf.deriveFont(14f));
        Color popupBackground = new Color(234, 242, 253);
        suggestedTitles.setBackground(popupBackground);

        popupScroller = new JScrollPane(suggestedTitles);
        Color popupBorderColor = new Color(186, 186, 186);
        Border popupBorder = new LineBorder(popupBorderColor, 1);
        popupScroller.setBorder(popupBorder);
        popupScroller.setBackground(popupBackground);

        popupWindow = new Window(this); // (this);
        popupWindow.setType(Type.POPUP);
        popupWindow.setLayout(new BorderLayout());
        popupWindow.add(popupScroller, BorderLayout.CENTER);
        popupWindow.setVisible(false);
        popupWindow.setAlwaysOnTop(true);
        popupWindow.setFocusable(false);
        suggestedTitles.setFocusable(false);

        suggestedTitlesModel.setFilterText("");

        addEscapeListener();

        SwingUtilities.invokeLater(titleEdit::requestFocus);
    }

    /**
     * Handles otherwise unhandled Esc in dialog.
     */
    private void addEscapeListener() {
        ActionListener escListener = e -> {
            if (!popupWindow.isVisible()) {
                setVisible(false);
            }
        };

        getRootPane().registerKeyboardAction(escListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

    }


    /**
     * Adds the appropriate components to the dialog, based on the options provided.
     */
    private JComponent layoutComponents() {
        JPanel dialogPanel = new JPanel();
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        dialogPanel.setLayout(new GridBagLayout());
        dialogPanel.setOpaque(true);
        dialogPanel.setBackground(dialogBackground);

        GridBagConstraints gbc = gbc();

        if (manyToOne) {
            manyToOneOk = new JCheckBox(String.format(
                "You are renaming %d Audio Items.\nAre you sure?", renameList.size()));
            manyToOneOk.addActionListener(comp -> enableOkButton());
            // If accepting the dialog on "Enter" is desired when this checkbox has
            // focus, do this:
            //manyToOneOk.addKeyListener(keyListener);
            dialogPanel.add(manyToOneOk, gbc);
        }

        renamePrompt = new JLabel();
        gbc.insets.bottom = 0;
        dialogPanel.add(renamePrompt, gbc);

        Box hbox = Box.createHorizontalBox();
        titleEdit = new JTextField(oldTitle);
        titleEdit.getDocument().addDocumentListener(titleDocumentListener);
        titleEdit.addComponentListener(titleComponentListener);
        titleEdit.addFocusListener(titleFocusListener);
        titleEdit.addKeyListener(titleKeyListener);
        hbox.add(titleEdit);
        hbox.add(Box.createHorizontalGlue());
        gbc = gbc();
        dialogPanel.add(hbox, gbc);

        if (renameMatching && renameList.size()>1) {
            // Show the "rename other matching titles" checkbox only if there was but a single
            // item provided. We don't do matching if multiple items were provided.
            String prompt = String.format("Also rename %d other matching instance(s).", renameList.size()-1);
            renameMatchingOk = new JCheckBox(prompt);
            renameMatchingOk.addActionListener(comp -> setRenamePrompt());
            renameMatchingOk.setToolTipText("At least one other message has this same title. Do you also want to rename "
                +"all other messages with this title?");
            dialogPanel.add(renameMatchingOk, gbc);
        }

        removeFromPlaylist = new JCheckBox("Remove from prior playlist");
        removeFromPlaylist.setToolTipText("Do you want to remove the message(s) from existing playlist(s) in which it(they) already occur?");
        dialogPanel.add(removeFromPlaylist, gbc);

        // If we have a program spec, and the new title is in the program spec, we'll be able
        // to add the title to the new playlist, if the user so wishes.
        if (haveProgramSpec) {
            addToPlaylist = new JCheckBox("Add to playlist");
            addToPlaylist.setToolTipText("Do you want to add the message(s) to any playlist(s) specified in the program specification?");
            dialogPanel.add(addToPlaylist, gbc);

            playlistList = new JList<>();
            playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            playlistList.setFocusable(false);
            JPanel playlistListPanel = new JPanel();
            playlistListPanel.setLayout(new BorderLayout());
            playlistListPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
            playlistListPanel.add(playlistList, BorderLayout.CENTER);
            dialogPanel.add(playlistListPanel, gbc);

            // And if we have a program spec, we can match spec'd titles with what the user has
            // typed so far. Set up the fuzzy matchers. (dialogPanel is passed for a debugging/
            // development feature.
            setupMatchers(dialogPanel);
        }

        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);

        return dialogPanel;
    }

    /**
     * Adds the OK and Cancel buttons to the dialog.
     */
    private JComponent layoutButtons() {
        Box hbox = Box.createHorizontalBox();
        hbox.setBorder(new EmptyBorder(4,10,4,8));
        hbox.add(Box.createHorizontalGlue());

        okButton = new JButton("Ok");
        okButton.addActionListener(e -> onOk());
        hbox.add(okButton);
        hbox.add(Box.createHorizontalStrut(15));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        hbox.add(cancelButton);
        hbox.add(Box.createHorizontalStrut(15));

        return hbox;
    }

    /**
     * Creates a typical GridBagConstraint
     * @return the GridBagConstraint.
     */
    private GridBagConstraints gbc() {
        Insets insets = new Insets(0, 0, 5, 0);
        return new GridBagConstraints(0, GridBagConstraints.RELATIVE,
            1, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 1,1);
    }

    /**
     * Returns a list of Audio Items matching (by title) the given item.
     * @param toMatch Item to be matched.
     * @return A List of matching items.
     */
    private List<AudioItem> findSameNamedItems(AudioItem toMatch) {
        MetadataStore store = ACMConfiguration
            .getInstance().getCurrentDB().getMetadataStore();
        // Find the ID of every item with the same title.
        oldTitle = toMatch.getTitle();
        SearchResult searchResult = store.search(oldTitle, null, null);
        // Turn into AudioItems, filter by matching titles. Original item needs to be first in result.
        List<AudioItem> result = new ArrayList<>();
        result.add(toMatch);
        searchResult
            .getAudioItems()
            .stream()
            .map(store::getAudioItem)
            .forEach(item -> {
                if (!result.contains(item) && item.getTitle().equalsIgnoreCase(oldTitle))
                    result.add(item);
            });
        return result;
    }

    /**
     * Sets the prompt, based on whether renaming one or multiple.
     */
    private void setRenamePrompt() {
        // If we may rename matching names, but user hasn't OK'd renaming the matching, then
        // we'll only rename one, however many match.
        int n = (renameMatching &&!renameMatchingOk.isSelected()) ? 1 : renameList.size();
        if (n > 1) {
            renamePrompt.setText(String.format("Enter new name for %d audio item(s):", n));
        } else {
            renamePrompt.setText("Enter new name for audio item:");
        }
        findMatchingPlaylists();
    }

    /**
     * Enables or disables the OK button based on other controls.
     */
    private void enableOkButton() {
        boolean enable = !manyToOne || manyToOneOk.isSelected();
        okButton.setEnabled(enable);
    }

    /**
     * Prepares for matching. We take the highest score from several fuzzy
     * matchers, and this sets up the matchers that we will use. Can set
     * checkboxes in the dialog, to allow experimenting with various combinations
     * of fuzzy matchers.
     * @param dialogPanel Where to put the fuzzy matcher checkboxes, if we're
     *                    doing that.
     */
    private void setupMatchers(JPanel dialogPanel) {
//        String[] matchNames = new String[] { "all", "ratio", "partialRatio", "tokenSortRatio",
//            "tokenSortPartialRatio", "tokenSetRatio", "tokenSetPartialRatio", "weightedRatio" };
        matchersEnabled = new HashMap<>();

        matchers = new LinkedHashMap<>();
        matchers.put("ratio", FuzzySearch::ratio);
        matchers.put("partialRatio", FuzzySearch::partialRatio);
        matchers.put("tokenSortRatio", FuzzySearch::tokenSortRatio);
        matchers.put("tokenSortPartialRatio", FuzzySearch::tokenSortPartialRatio);
        matchers.put("tokenSetRatio", FuzzySearch::tokenSetRatio);
        matchers.put("tokenSetPartialRatio", FuzzySearch::tokenSetPartialRatio);
        matchers.put("weightedRatio", FuzzySearch::weightedRatio);
        matchers.keySet().forEach(str -> matchersEnabled.put(str, false));
        matchersEnabled.put("partialRatio", true);
        matchersEnabled.put("tokenSortPartialRatio", true);

//        GridBagConstraints gbc = gbc();
        for (String name : matchers.keySet()) {
            JCheckBox matcherCb = new JCheckBox(name, matchersEnabled.get(name));
            matcherCb.addActionListener(e -> {
                JCheckBox cb = (JCheckBox) e.getSource();
                matchersEnabled.put(name, cb.isSelected());
                titleEdit.requestFocus();
            });
//            dialogPanel.add(matcherCb, gbc);
        }

    }

    /**
     * Applies the fuzzy match algorithms to two strings.
     * @param a One string.
     * @param b The other.
     * @return The highest match score between the two strings.
     */
    private int match(String a, String b) {
        Map<String, Integer> matchingStrategies = new LinkedHashMap<>();
        int bestScore = -1;
        for (String name : matchers.keySet()) {
            int score;
            try {
                score = matchers.get(name).apply(a, b);
            } catch (Exception ignored) {
                score = 0;
            }
            if (score >= 60) matchingStrategies.put(name, score);
            if (matchersEnabled.get(name)) {
                if (score > bestScore) {
                    bestScore = score;
                }
            }
        }
        if (bestScore > 60) {
            System.out.printf("%s :: %s ==> ", a, b);
            for (String name : matchingStrategies.keySet())
                System.out.printf("%s=%d ", name, matchingStrategies.get(name));
            System.out.println();
        }
        return bestScore;
    }

    /**
     * Given a message title, get all of the playlist titles in which the message should appear. We're also given a
     * list of audio items, which are the messages being renamed. Get the languages of those messages, to find the
     * relevant playlists.
     * @param title of the message.
     * @return a list of { AudioItem, String } pairs, mapping messages to playlist titles.
     */
    private List<Triple<AudioItem, ContentSpec.MessageSpec, String>> getPlaylistsForItemWithTitle(List<AudioItem> items, String title) {
        List<Triple<AudioItem, ContentSpec.MessageSpec, String>> result = new ArrayList<>();
        // Get the MesageSpec(s) for the title (title in all deployments)
        List<ContentSpec.MessageSpec> specList = messageSpecs.get(title);

        // For all of the message specs that match the title... (one message spec for
        // every deployment in which the title occurs)
        for (ContentSpec.MessageSpec ms : specList) {
            // For every item that was selected for renaming (generally, hopefully, one for each language) ...
            for (AudioItem item : items) {
                // If the item's language is included in the playlist (per program spec)...
                String languagecode = item.getLanguageCode();
                if (ms.includesLanguage(languagecode)) {
                    // return that {item : playlist name} pair.
                    result.add(new ImmutableTriple<>(item, ms, AudioUtils.decoratedPlaylistName(
                        ms.getPlaylistTitle(),
                        ms.deploymentNumber,
                        languagecode)));
                }
            }
        }

        return result;
    }

    /**
     * Determine whether items have old playlists from which they can be removed, and/or
     * new playlists to which they can be added.
     */
    private void findMatchingPlaylists() {
        String newTitle = titleEdit.getText().trim();
        // Message specs for all messages in the program spec.
        List<ContentSpec.MessageSpec> specList = messageSpecs.get(newTitle);
        List<AudioItem> toRename = getItemsToRename();

        // If any audio item was in playlist(s), ask if user wants to remove them.
        boolean hadPreviousPlaylist = toRename.stream().anyMatch(ai -> ai.getPlaylists().size()>0);
        removeFromPlaylist.setSelected(hadPreviousPlaylist);
        removeFromPlaylist.setEnabled(hadPreviousPlaylist);

        // If no program spec, no matching playlists; the remaining components are not even populated.
        if (!haveProgramSpec) return;

        if (specList == null || specList.size() == 0) {
            // New title is not in program spec, so no playlists from program spec.
            addToPlaylist.setSelected(false);
            addToPlaylist.setEnabled(false);
            playlistList.setListData(new String[0]);
        } else {
            // Title is in one or more playlists.
            addToPlaylist.setSelected(true);
            addToPlaylist.setEnabled(true);

            Vector<String> newPlaylists = getPlaylistsForItemWithTitle(toRename, newTitle)
                    .stream()
                    .map(Triple::getRight)
                    .distinct()
                    .collect(Collectors.toCollection(Vector::new));

            playlistList.setListData(newPlaylists);
            playlistList.setVisible(newPlaylists.size() > 0);
        }
    }

    /**
     * List model for the suggested titles. Content provided by specTitles,
     * possibly filtered.
     */
    private class suggestedTitlesModel extends AbstractListModel<String> {
        private List<String> filteredList = null;
        private Map<String, List<String>> filterHistory = new HashMap<>();
        private String prevFilter = null;

        @Override
        public int getSize() {
            if (filteredList != null) return filteredList.size();
            return specTitles.size();
        }

        @Override
        public String getElementAt(int index) {
            if (filteredList != null) return filteredList.get(index);
            return specTitles.get(index);
        }

        private List<String> getEffectiveList() {
            if (filteredList != null) return filteredList;
            return specTitles;
        }

        @Override
        public void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1);
        }

        void fireContentChanged() {
            super.fireContentsChanged(this, 0, getSize());
        }

        /**
         * Sets the filter text, when user types, pastes, or deletes text from the titleEdit.
         *
         * Makes pretty loose matches to fill the suggestion. If the filter occurs in any progspec
         * title, that title is shown. If the filter fuzzy matches any progspec title, that title
         * is show (and tries a couple of different fuzzy matchers, just to provide more
         * opportunities for a match).
         * @param text of the new filter.
         */
        private void setFilterText(String text) {
            if (StringUtils.isBlank(text)) {
                filteredList = null;
                filterHistory.clear();
                prevFilter = null;
            } else {
                if (StringUtils.isNotEmpty(prevFilter) && text.length() == prevFilter.length() + 1
                    && text.startsWith(prevFilter)) {
                    // User added a single character. Save previous result as history.
                    filterHistory.put(prevFilter, new ArrayList<>(getEffectiveList()));
                } else if (StringUtils.isNotEmpty(prevFilter)
                    && prevFilter.length() == text.length() + 1 && prevFilter.startsWith(text)
                    && filterHistory.containsKey(text)) {
                    // If user deleted last character, and we have a history, use it.
                    filteredList = filterHistory.get(text);
                    filterHistory.remove(text);
                } else {
                    // Not a simple add-1 or remove-1 at the end. New search.
                    filteredList = null;
                    filterHistory.clear();
                }

                if (specTitles.contains(text)) {
                    // There is an exact match. Make that match the one-and-only suggestion.
                    filteredList = Collections.singletonList(text);
                } else {
                    // This starts with the current filteredList (or full list if not filtered list),
                    // and filters it.
                    filteredList = getEffectiveList().stream().filter(title -> {
                        if (StringUtils.containsIgnoreCase(title, text)) return true;
                        int score = match(title, text);
                        return score > 60;
                    }).collect(Collectors.toList());
                }
                prevFilter = text;
            }
            
            fireContentChanged();
            refreshPopup();
            findMatchingPlaylists();
        }
    }

    private suggestedTitlesModel suggestedTitlesModel = new suggestedTitlesModel();

    /**
     * If the popup isn't showing, but should be, show it. Refresh the contents of
     * the popup.
     */
    private void refreshPopup() {
        // If no program spec, no possible titles with which to prompt.
        if (!haveProgramSpec) return;

        boolean show = true;

        // If there is nothing to suggest, or the only thing to suggest is what is already in
        // the titleEdit box, don't show the popup.
        if (suggestedTitlesModel.getSize() == 0 || (suggestedTitlesModel.getSize() == 1 &&
                    suggestedTitlesModel.getElementAt(0).equals(titleEdit.getText()))) {
            show = false;
        }

        // If the title textEdit doesn't have focus, don't show the popup.
        if (!titleEdit.hasFocus()) {
            show = false;
        }

        // If we don't want to show, hide the window now.
        if (!show) {
            popupWindow.setVisible(false);
            return;
        }

        Rectangle deviceBounds = popupWindow.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();

        suggestedTitles.clearSelection();
        Point p = titleEdit.getLocationOnScreen();
        p.y += titleEdit.getHeight();
        int availableHeight = deviceBounds.y + deviceBounds.height - p.y - 60;
        Dimension desiredSize = suggestedTitles.getPreferredSize();
        desiredSize.height = Math.min(desiredSize.height + 2, availableHeight);
        desiredSize.width += 100; // allow for scrollbar
        popupWindow.setSize(desiredSize);
        popupWindow.setLocation(p);
        popupWindow.setVisible(true);
        popupWindow.setEnabled(true);
    }

    /**
     * Hide and remove the popup.
     */
    private void hidePopup() {
        popupWindow.setVisible(false);
    }


    /**
     * Listen for changes to the titleEdit, and update the filter accordingly.
     */
    private DocumentListener titleDocumentListener = new DocumentListener() {
        private void setText() {
            suggestedTitlesModel.setFilterText(titleEdit.getText());
            findMatchingPlaylists();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            setText();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            setText();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            setText();
        }
    };

    /**
     * Listen for show/hide/move events, and show/hide/refresh the popup as
     * appropriate.
     */
    private ComponentListener titleComponentListener = new ComponentListener() {
        @Override
        public void componentResized(ComponentEvent e) {
            refreshPopup();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            refreshPopup();
        }

        @Override
        public void componentShown(ComponentEvent e) {
            refreshPopup();
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            hidePopup();
        }
    };

    /**
     * Listen for focus events on titleEdit, and show the popup when the
     * edit has focus. Select the full text when the edit gains focus.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final FocusListener titleFocusListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            refreshPopup();
            titleEdit.selectAll();
        }

        @Override
        public void focusLost(FocusEvent e) {
            hidePopup();
        }
    };

    /**
     * Listen for keystrokes on the titleEdit, and handle enter, escape, up, and down.
     */
    private KeyListener titleKeyListener = new KeyAdapter() {

        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == '\n') {
                // If the popup window is visible, copy any selected title (from progspec) into
                // the titleEdit. If the popup window is NOT visible, treat as clicking OK.
                if (popupWindow.isVisible()) {
                    String selection = suggestedTitles.getSelectedValue();
                    if (StringUtils.isNotEmpty(selection)) {
                        try {
                            adjustingSelection = true;
                            titleEdit.setText(selection);
                        } finally {
                            adjustingSelection = false;
                        }
                    }
                    e.consume();
                } else if (okButton.isEnabled()) {
                    onOk();
                }
            } else if (e.getKeyChar() == '\u001b') {
                // If the popup window is visible, close the suggestedTitles popup. If the
                // window is NOT visible, treat as clicking Cancel.
                if (popupWindow.isVisible()) {
                    hidePopup();
                    e.consume();
                } else {
                    // Close on escape.
                    setVisible(false);
                }
            } else {
                super.keyTyped(e);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            super.keyPressed(e);
            if (!popupWindow.isVisible()) return;
            try {
                adjustingSelection = true;
                int ix = suggestedTitles.getSelectedIndex();
                int newIx = ix;
                int code = e.getKeyCode();
                if (code == VK_DOWN) {
                    if (ix < suggestedTitlesModel.getSize() - 1) {
                        newIx = ix + 1;
                    }
                } else if (code == VK_UP) {
                    if (ix > 0) {
                        newIx = ix - 1;
                    }
                }
                if (newIx != ix) {
                    suggestedTitles.setSelectedIndex(newIx);
                    suggestedTitles.ensureIndexIsVisible(newIx);
                    // This is clearly bogus. However, without it, the JList frequently (1 time
                    // in 5?) fails to repaint the highlight to match the selection. At least this
                    // is better than setVisible(false);setVisible(true);
                    Rectangle r = suggestedTitles.getCellBounds(Math.max(ix,0), newIx);
                    suggestedTitles.getParent().repaint(r.x, r.y, r.width, r.height);
                }
            } finally {
                adjustingSelection = false;
            }
        }

    };

    /**
     * Listens for selection on the suggested titles, and fills the titleEdit
     * with whatever is selected. This lets the user click on a suggestion to
     * accept it.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ListSelectionListener titleSelectionListener = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {    
            if (adjustingSelection) {
                return;
            }
            String selection = suggestedTitles.getSelectedValue();
            if (selection == null) return;
            titleEdit.setText(selection);
        }
    };


    /**
     * Gets the list of Audio Items to be renamed, based on the arguments, matching items,
     * and state of components.
     * @return the net list of items to be changed.
     */
    private List<AudioItem> getItemsToRename() {
        if (renameMatching && !renameMatchingOk.isSelected()) {
            return renameList.subList(0, 1);
        } else {
            return renameList;
        }
    }

    /**
     * Do the work.
     */
    private void onOk() {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        // The interaction between Playlists, AudioItems, and Transactions isn't obvious.
        // To remove an audio item from a playlist (without deleting the playlist), delete the
        // playlist from the audio item, delete the audio item from the playlist, and commit
        // the audio item.
        // When adding an audio item to a playlist, commit the audio item and the playlist.
        // When adding a playlist, commit the playlist, which will also commit any audio items
        // added.
        // It is (apparently) OK to add a committable more than once.
        Transaction transaction = store.newTransaction();
        String newTitle = titleEdit.getText().trim();

        List<AudioItem> toRename = getItemsToRename();
        // Set the new title.
        toRename.forEach(item -> {
            item.getMetadata().put(MetadataSpecification.DC_TITLE, newTitle);
            transaction.add(item);
        });

        // Adjust playlists.
        if (removeFromPlaylist.isSelected()) {
            toRename.forEach(item -> {
                // Copy to a new collection because we will modify the existing collection.
                Collection<Playlist> playlists = new ArrayList<>(item.getPlaylists());
                playlists.forEach(pl -> {
                    item.removePlaylist(pl);
                    pl.removeAudioItem(item.getId());
                    transaction.add(pl);
                });
                transaction.add(item);
            });
        }
        if (addToPlaylist != null && addToPlaylist.isSelected()) {
            getPlaylistsForItemWithTitle(toRename, newTitle)
                .forEach(triple -> {
                    AudioItem item = triple.getLeft();
                    ContentSpec.MessageSpec messageSpec = triple.getMiddle();
                    String playlistName = triple.getRight();
                    Playlist playlist = store.findPlaylistByName(playlistName);
                    if (playlist == null) {
                        playlist = store.newPlaylist(playlistName);
                    }
                    // Where does the item go in the playlist?
                    int index = AudioUtils.findIndexForMessageInPlaylist(messageSpec, playlist, item.getLanguageCode());
                    playlist.addAudioItem(index, item);
                    transaction.add(playlist);
                    transaction.add(item);
                });
        }

        // All or none.
        try {
            transaction.commit();
        } catch (IOException e) {
            // Ignore. e.printStackTrace();
        }
        Application.getFilterState().updateResult(true);
        setVisible(false);
    }

    /**
     * Load the program spec, and populate specTitles.
     */
    private void loadProgramSpec() {
        specTitles.clear();
        this.messageSpecs.clear();
        File programSpecDir = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getPathProvider()
                .getProgramSpecDir();
        ProgramSpec programSpec = new ProgramSpec(programSpecDir);

        ContentSpec content = programSpec.getContentSpec();
        if (content != null) {
            // Find all the titles, and their message specs. (From message spec, we can get playlist.)
            Map<String, List<ContentSpec.MessageSpec>> foundMessageSpecs = new HashMap<>();
            for (ContentSpec.DeploymentSpec ds : content.getDeploymentSpecs())
                for (ContentSpec.PlaylistSpec ps : ds.getPlaylistSpecs())
                    for (ContentSpec.MessageSpec ms : ps.getMessageSpecs()) {
                        // Keep track of all the messages (ie, in multiple deployments) with the title.
                        List<ContentSpec.MessageSpec> msList = foundMessageSpecs.computeIfAbsent(ms.getTitle(),
                            k -> new ArrayList<>());
                        msList.add(ms);
                    }
            // Sort by title.
            specTitles.addAll(foundMessageSpecs.keySet());
            specTitles.sort(String::compareToIgnoreCase);
            specTitles.forEach(str -> this.messageSpecs.put(str, foundMessageSpecs.get(str)));
            suggestedTitlesModel.fireContentChanged();
            haveProgramSpec = this.messageSpecs.size()>0;
        }
    }

}

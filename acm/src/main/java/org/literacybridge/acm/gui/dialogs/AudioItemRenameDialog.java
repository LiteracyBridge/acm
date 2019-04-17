package org.literacybridge.acm.gui.dialogs;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.SearchResult;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog to rename multiple Audio Items. There are two ways that it can operate:
 * 1) Multiple audio items passed in, to receive a new name. User will be prompted
 *    to enter a name, based on the first item passed.
 * 2) One item passed in. Search for any other Audio Items that have the same name,
 *    and rename them all together.
 */
public class AudioItemRenameDialog extends JDialog {

    private final JPanel dialogPanel;
    private JLabel renamePrompt;
    private JCheckBox manyToOneOk;
    private JCheckBox renameMatchingOk;
    private JTextField titleEdit;
    private JButton okButton;

    private boolean manyToOne = false;
    private boolean renameMatching = false;
    private String oldTitle;
    private List<AudioItem> renameList;

    public AudioItemRenameDialog(final JFrame parent, AudioItem... audioItems) {
        super(parent, "Rename Audio Item", true);
        dialogPanel = new JPanel();
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(15,15,10,15));
        dialogPanel.setLayout(new GridBagLayout());
        add(dialogPanel);

        if (audioItems.length == 0) return;
        oldTitle = audioItems[0].getTitle();

        List<AudioItem> audioItems1 = Arrays.asList(audioItems);
        if (audioItems1.size() == 1) {
            renameList = findMatchingItems(audioItems[0]);
            renameMatching = renameList.size()>1;
        } else {
            renameList = audioItems1;
            manyToOne = true;
        }

        layoutComponents();
        addButtons();

        setRenamePrompt();
        enableOkButton();

        setResizable(true);
        setSize(getPreferredSize());

        SwingUtils.addEscapeListener(this);
    }

    /**
     * Adds the appropriate components to the dialog, based on the options provided.
     */
    private void layoutComponents() {
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
        titleEdit.addKeyListener(keyListener);
        hbox.add(titleEdit);
        hbox.add(Box.createHorizontalGlue());
        gbc = gbc();
        dialogPanel.add(hbox, gbc);

        if (renameMatching && renameList.size()>1) {
            String prompt = String.format("Also rename %d other matching instance(s).", renameList.size()-1);
            renameMatchingOk = new JCheckBox(prompt);
            renameMatchingOk.addActionListener(comp -> setRenamePrompt());
            // Only show the checkbox if there was only a single item provided. We don't do matching
            // if multiple items were provided.
            dialogPanel.add(renameMatchingOk, gbc);
        }

        gbc.weighty = 1.0;
        dialogPanel.add(new JLabel(""), gbc);
    }

    /**
     * Adds the OK and Cancel buttons to the dialog.
     */
    private void addButtons() {
        Box hbox = Box.createHorizontalBox();
        hbox.add(Box.createHorizontalGlue());

        okButton = new JButton("Ok");
        okButton.addActionListener(e -> onOk());
        hbox.add(okButton);
        hbox.add(Box.createHorizontalStrut(5));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        hbox.add(cancelButton);

        GridBagConstraints gbc = gbc();
        gbc.insets.top = 5;
        gbc.insets.bottom = 0;
        dialogPanel.add(hbox, gbc);
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
    private List<AudioItem> findMatchingItems(AudioItem toMatch) {
        MetadataStore store = ACMConfiguration
            .getInstance().getCurrentDB().getMetadataStore();
        // Find the ID of every item with the same title.
        oldTitle = toMatch.getTitle();
        SearchResult searchResult = store.search(oldTitle, null, null);
        // Turn into AudioItems, and set the title.
        return searchResult
            .getAudioItems()
            .stream()
            .map(store::getAudioItem)
            .filter(item -> item.getTitle().equalsIgnoreCase(oldTitle))
            .collect(Collectors.toList());
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
    }

    /**
     * Enables or disables the OK button based on other controls.
     */
    private void enableOkButton() {
        boolean enable = !manyToOne || manyToOneOk.isSelected();
        okButton.setEnabled(enable);
    }

    private KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == '\n' && okButton.isEnabled()) {
                onOk();
            } if (e.getKeyChar() == '\u001b') {
                // Close on escape.
                setVisible(false);
            } else {
                super.keyTyped(e);
            }
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
        String newTitle = titleEdit.getText().trim();

        List<AudioItem> toRename = getItemsToRename();
        // Set the new title.
        toRename.forEach(item -> item.getMetadata().put(MetadataSpecification.DC_TITLE, newTitle));

        // All or none.
        try {
            store.commit(toRename.toArray(new AudioItem[0]));
        } catch (IOException e) {
            // Ignore. e.printStackTrace();
        }

        setVisible(false);
    }

}

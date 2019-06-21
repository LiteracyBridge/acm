package org.literacybridge.acm.gui.dialogs;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.MainWindow.PlaylistListModel;
import org.literacybridge.acm.gui.MainWindow.SidebarView;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.store.Transaction;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.literacybridge.acm.gui.MainWindow.PlaylistListModel.PlaylistLabel;
import static org.literacybridge.acm.utils.SwingUtils.getApplicationRelativeLocation;

public final class PlaylistEditDialog extends JDialog {
    private PlaylistLabel playlistLabel;
    private List<String> originalItemIds;
    private List<String> modifiedItemIds;

    private Map<String, String> labelsMap = new HashMap<>();

    private final MyListModel listModel;
    private final JList<String> itemsList;
    private JButton moveUpButton;
    private JButton removeButton;
    private JButton moveDownButton;

    private static String makeTitle(PlaylistLabel playlistLabel) {
        return String.format("Edit Playlist '%s'", playlistLabel.toString());
    }

    private PlaylistEditDialog(Window owner, PlaylistLabel playlist) {
        super(owner, makeTitle(playlist), ModalityType.DOCUMENT_MODAL);

        // The data we manipulate
        this.playlistLabel = playlist;
        modifiedItemIds = new ArrayList<>(playlist.getPlaylist().getAudioItemList());
        originalItemIds = new ArrayList<>(modifiedItemIds);
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        modifiedItemIds.forEach(it -> labelsMap.put(it, store.getAudioItem(it).getTitle()));

        // The GUI
        setLayout(new BorderLayout());
        add(setupDoneButtons(), BorderLayout.SOUTH);

        JPanel dialogPanel = new JPanel();
        add(dialogPanel, BorderLayout.CENTER);
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setBorder(new EmptyBorder(5, 5, 10, 5));
        dialogPanel.add(setupEditButtons(), BorderLayout.SOUTH);
        Color background = new Color(0xe0f7ff);
        dialogPanel.setBackground(background);

        listModel = new MyListModel();
        itemsList = new JList<>(listModel);
        itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemsList.setCellRenderer(listCellRenderer);
        itemsList.addListSelectionListener(this::selectionListener);
        itemsList.addFocusListener(listFocusListener);
        JScrollPane itemsListScrollPane = new JScrollPane(itemsList);
        dialogPanel.add(itemsListScrollPane, BorderLayout.CENTER);

        selectionListener(null);

        SwingUtils.addEscapeListener(this);
        setMinimumSize(new Dimension(500, 300));
    }

    /**
     * Creates the OK and Cancel buttons, and wires up their listeners.
     * @return a horizontal box containing the buttons. Right justified.
     */
    private JComponent setupDoneButtons() {
        Box hbox = Box.createHorizontalBox();
        hbox.add(Box.createHorizontalGlue());

        JButton okButton = new JButton("Ok");
        okButton.addActionListener(this::onOk);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this::onCancel);

        hbox.add(okButton);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.add(cancelButton);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.setBorder(new EmptyBorder(4,10,4,8));
        return hbox;
    }

    /**
     * Creates the move up, remove, and move down, and wires up their listeners.
     * @return a horizontal box containing the buttons. Right justified.
     */
    private JComponent setupEditButtons() {
        Box hbox = Box.createHorizontalBox();

        moveUpButton = new JButton("Move Up");
        moveUpButton.addActionListener(actionEvent -> listModel.moveUp(itemsList.getSelectedIndex()));

        removeButton = new JButton("Remove Item");
        removeButton.addActionListener(actionEvent -> listModel.remove(itemsList.getSelectedIndex()));

        moveDownButton = new JButton("Move Down");
        moveDownButton.addActionListener(actionEvent -> listModel.moveDown(itemsList.getSelectedIndex()));

        hbox.add(moveUpButton);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.add(removeButton);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.add(moveDownButton);
        hbox.add(Box.createHorizontalGlue());
        hbox.setBorder(new EmptyBorder(4, 10, 4, 10));
        return hbox;
    }

    /**
     * If the user made any net changes, commit them.
     * @param e is unused.
     */
    private void onOk(ActionEvent e) {
        // Commit any changes and exit.
        boolean success = false;
        Playlist playlist = playlistLabel.getPlaylist();
        Transaction transaction = ACMConfiguration.getInstance()
            .getCurrentDB()
            .getMetadataStore()
            .newTransaction();

        try {
            // Deletes first.
            boolean haveDeletes = false;
            for (String audioItemId : originalItemIds) {
                if (!modifiedItemIds.contains(audioItemId)) {
                    AudioItem audioItem = ACMConfiguration.getInstance()
                        .getCurrentDB()
                        .getMetadataStore()
                        .getAudioItem(audioItemId);
                    audioItem.removePlaylist(playlist);
                    playlist.removeAudioItem(audioItemId);
                    transaction.add(audioItem);
                    haveDeletes = true;
                }
            }
            // See if anything was re-arranged. Ignore any deleted items; are the remaining items
            // in the same order as they were previously?
            int prevOldIx = -2;
            boolean haveReorders = false;
            for (String modifiedItemId : modifiedItemIds) {
                int oldIx = originalItemIds.indexOf(modifiedItemId);
                if (oldIx < prevOldIx) {
                    haveReorders = true;
                    break;
                }
                prevOldIx = oldIx;
            }
            if (haveReorders) {
                modifiedItemIds.forEach(playlist::removeAudioItem);
                modifiedItemIds.forEach(playlist::addAudioItem);
            }
            if (haveDeletes || haveReorders) {

                transaction.add(playlist);
                transaction.commit();
                success = true;
            }
        } catch (IOException ex) {
            // Ignore.
        } finally {
            if (!success) {
                try {
                    transaction.rollback();
                } catch (IOException ex) {
                    // Couldn't roll back.
                    // TODO: pop up a message suggesting abandoning changes.
                }
            }
            Application.getMessageService().pumpMessage(new SidebarView.PlaylistsChanged());
            Application.getFilterState().updateResult(true);
        }

        setVisible(false);
    }

    private void onCancel(ActionEvent e) {
        // Abandon any changes and exit.
        setVisible(false);
    }

    /**
     * Enable / disable buttons based on current selection (if any).
     * @param listSelectionEvent the selection event.
     */
    private void selectionListener(ListSelectionEvent listSelectionEvent) {
        if (listSelectionEvent != null && listSelectionEvent.getValueIsAdjusting()) return;
        int curSelection = itemsList.getSelectedIndex();
        int curSize = itemsList.getModel().getSize();

        if (curSelection < 0) {
            moveUpButton.setEnabled(false);
            removeButton.setEnabled(false);
            moveDownButton.setEnabled(false);
        } else {
            moveUpButton.setEnabled(curSelection > 0);
            removeButton.setEnabled(true);
            moveDownButton.setEnabled(curSelection < curSize-1);
        }
    }

    /**
     * Paint a "focus border" when the list has focus.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final FocusListener listFocusListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            setListBorder();
        }

        @Override
        public void focusLost(FocusEvent e) {
            setListBorder();
        }
    };

    private Color borderColor = new Color(136, 176, 220);
    private Border focusBorder = new RoundedLineBorder(borderColor, 2, 6);
    private Border noFocusBorder = new RoundedLineBorder(borderColor, 1, 6, 2);
    private void setListBorder() {
        Border border = itemsList.hasFocus() ? focusBorder : noFocusBorder;
        itemsList.setBorder(border);
    }

    private class MyListModel extends AbstractListModel<String> {
        @Override
        public int getSize() {
            return modifiedItemIds.size();
        }

        @Override
        public String getElementAt(int index) {
            return modifiedItemIds.get(index);
        }

        /**
         * Move up in the list, to a lower index.
         * @param index to be moved up
         */
        private void moveUp(int index) {
            if (index < 1 || index >= modifiedItemIds.size()) return ;
            doMove(index, index-1);
        }

        /**
         * Move down in the list, that is, to a higher index.
         * @param index to be moved down
         */
        private void moveDown(int index) {
            if (index < 0 || index >= modifiedItemIds.size()-1) return ;
            doMove(index, index+1);
        }

        private void doMove(int oldIndex, int newIndex) {
            String savedItem = modifiedItemIds.get(oldIndex);

            modifiedItemIds.remove(oldIndex);
            fireIntervalRemoved(this, oldIndex, oldIndex);

            modifiedItemIds.add(newIndex, savedItem);
            fireIntervalAdded(this, newIndex, newIndex);

            itemsList.setSelectedIndex(newIndex);
        }

        private void remove(int index) {
            if (index < 0 || index >= modifiedItemIds.size()) return ;
            modifiedItemIds.remove(index);
            fireIntervalRemoved(this, index, index);

            itemsList.setSelectedIndex(index);
        }
    }

    /**
     * Render items as their title. Also paint zebra stripes.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private ListCellRenderer<? super String> listCellRenderer = new DefaultListCellRenderer() {

        @Override
        public Component getListCellRendererComponent(JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            String label = labelsMap.get(value.toString());
            Component comp = super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            if (isSelected)
                comp.setBackground(AcmAssistantPage.bgSelectionColor);
            else
                comp.setBackground(index%2==0 ? AcmAssistantPage.bgColor : AcmAssistantPage.bgAlternateColor);
            return comp;
        }
    };


    public static void showDialog(PlaylistLabel playlist) {
        PlaylistEditDialog dialog = new PlaylistEditDialog(Application.getApplication(), playlist);
        // Place the new dialog within the application frame. This is hacky, but if it fails, the dialog
        // simply winds up in a funny place. Unfortunately, Swing only lets us get the location of a
        // component relative to its parent.
        Point pAudio = getApplicationRelativeLocation(Application.getApplication()
            .getMainView()
            .getAudioItemView());
        dialog.setLocation(pAudio);
        dialog.setVisible(true);
    }

}

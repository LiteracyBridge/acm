package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog allows user to manually select the matching file or audio item for a selected
 * audio item or file.
 */
public abstract class AbstractManualMatcherDialog<T extends MatchableItem> {
    private final JList<String> choicesList;
    private final JLabel promptLabel;
    private JButton okButton, cancelButton;
    private JDialog dialog;
    private final JOptionPane optionPane;
    private final Map<String, T> unmatchedItems;

    public AbstractManualMatcherDialog() {
        promptLabel = new JLabel("");

        unmatchedItems = new LinkedHashMap<>();
        choicesList = new JList<>();
        choicesList.addListSelectionListener(this::listSelectionListener);
        choicesList.addMouseListener(listMouseListener);
        choicesList.setCellRenderer(listCellRenderer);

        setupButtons();
        optionPane = new JOptionPane();
        optionPane.setOptions(new Object[] { okButton, cancelButton });
        dialog = optionPane.createDialog("");

        SwingUtils.addEscapeListener(dialog);
    }

    /**
     * Display an unmatched item from one side and a list of unmatched items from the other side.
     * @param row The MatchableItem item to be matched. Must be LEFT_ONLY or RIGHT_ONLY.
     * @param matchableItems all of the MatchableItem items. The other-side unmatched items will
     *                       be offered.
     * @return The chosen match.
     */
    public T chooseMatchFor(T row, List<T> matchableItems) {
        MATCH filter;
        String sourcesDescription, targetDescription;
        String targetName;
        final boolean isLeft = (row.getMatch() == MATCH.LEFT_ONLY);
        if (row.getMatch() == MATCH.LEFT_ONLY) {
            filter = MATCH.RIGHT_ONLY;
            targetDescription = rightDescription();
            sourcesDescription = leftDescription();
            targetName = row.getLeft().toString();
        } else if (row.getMatch() == MATCH.RIGHT_ONLY) {
            filter = MATCH.LEFT_ONLY;
            targetDescription = leftDescription();
            sourcesDescription = rightDescription();
            targetName = row.getRight().toString();
        } else {
            throw new IllegalArgumentException("Can only choose a match for an unmatched item");
        }

        matchableItems
            .stream()
            .filter(item->item.getMatch()== filter)
            .forEach(item-> unmatchedItems.put((isLeft?item.getRight():item.getLeft()).toString(), item));
        String[] unmatchedTitles = unmatchedItems
            .keySet().toArray(new String[0]);

        choicesList.setListData(unmatchedTitles);
        /* bad */
        String message = String.format("<html>Choose the %s for %s <br/>&nbsp;&nbsp;<i>%s</i>.</html>", targetDescription, sourcesDescription, targetName);
        promptLabel.setText(message);

        JPanel pane = layoutComponents();
        optionPane.setMessage(pane);
        dialog = optionPane.createDialog(String.format("Choose %s for %s", targetDescription, sourcesDescription));

        return show();

    }

    /**
     * Display name for a left-side item. This is generally the "target" side, but in a match operation
     * either side can be the "source" or the "target".
     * @return the name.
     */
    protected abstract String leftDescription();

    /**
     * Display name for a right-side item.
     * @return the name.
     */
    protected abstract String rightDescription();

    /**
     * Mouse listener so we can accept a match on a double click.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private MouseListener listMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) dialog.setVisible(false);
        }
    };

    /**
     * Create the OK and Cancel buttons and their listeners.
     */
    private void setupButtons() {
        okButton = new JButton("Ok");
        okButton.addActionListener(e -> dialog.setVisible(false));
        okButton.setEnabled(false);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            choicesList.clearSelection();
            dialog.setVisible(false);
        });
    }

    /**
     * Create a panel with a prompt, a list, and two buttons.
     * @return The panel.
     */
    private JPanel layoutComponents() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(promptLabel, BorderLayout.NORTH);
        panel.add(choicesList, BorderLayout.CENTER);
        choicesList.setBorder(new LineBorder(Color.red, 1));
        return panel;
    }

    /**
     * Listens for list selection, and enables the OK button whenever an item is selected.
     * @param listSelectionEvent isunused.
     */
    private void listSelectionListener(ListSelectionEvent listSelectionEvent) {
        okButton.setEnabled(choicesList.getLeadSelectionIndex() >= 0);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private ListCellRenderer<? super String> listCellRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (isSelected)
                comp.setBackground(AcmAssistantPage.bgSelectionColor);
            else
                comp.setBackground(index%2==0 ? AcmAssistantPage.bgColor : AcmAssistantPage.bgAlternateColor);
            return comp;
        }
    };

    public T show() {
        dialog.setVisible(true);
        return getSelectedItem();
    }

    private T getSelectedItem() {
        String chosenTitle = choicesList.getSelectedValue();
        if (chosenTitle != null) {
            return unmatchedItems.get(chosenTitle);
        }
        return null;
    }

}


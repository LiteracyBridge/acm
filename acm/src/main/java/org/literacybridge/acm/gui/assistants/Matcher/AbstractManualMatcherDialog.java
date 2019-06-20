package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.assistants.ContentImport.AudioPlaylistTarget;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.redBorder;

/**
 * Dialog allows user to manually select the matching file or audio item for a selected
 * audio item or file.
 */
public abstract class AbstractManualMatcherDialog<T extends MatchableItem> extends JDialog {
    private final JList<String> choicesList;
    private final JLabel promptLabel;
    private JButton okButton;
    private final Map<String, T> unmatchedItems;
    private JScrollPane choicesListScrollPane;

    public AbstractManualMatcherDialog(T row, List<T> matchableItems) {
        super((Frame)null, "", true);
        setLayout(new BorderLayout());
        JPanel dialogPanel = new JPanel();
        add(dialogPanel, BorderLayout.CENTER);
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        promptLabel = new JLabel("");

        unmatchedItems = new LinkedHashMap<>();
        choicesList = new JList<>();
        choicesList.addListSelectionListener(this::listSelectionListener);
        choicesList.addFocusListener(listFocusListener);
        choicesList.addMouseListener(listMouseListener);
        choicesList.setCellRenderer(listCellRenderer);

        dialogPanel.add(setupButtons(), BorderLayout.SOUTH);
        dialogPanel.add(buildLayout(row, matchableItems));

        getRootPane().registerKeyboardAction(e -> onCancel(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        ActionListener enterListener = (e) -> {
            System.out.println("Enter");
            onOk();
        };

        getRootPane().registerKeyboardAction(enterListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().setDefaultButton(null);

        setMinimumSize(new Dimension(500, 300));
        setVisible(true);
    }

    /**
     * Display an unmatched item from one side and a list of unmatched items from the other side.
     * @param row The MatchableItem item to be matched. Must be LEFT_ONLY or RIGHT_ONLY.
     * @param matchableItems all of the MatchableItem items. The other-side unmatched items will
     *                       be offered.
     * @return The chosen match.
     */
    private JComponent buildLayout(T row, List<T> matchableItems) {
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
        setTitle(String.format("Choose %s for %s", targetDescription, sourcesDescription));

        return pane;

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
            if (e.getClickCount() == 2) setVisible(false);
        }
    };

    /**
     * Create the OK and Cancel buttons and their listeners.
     */
    private JComponent setupButtons() {
        Box hbox = Box.createHorizontalBox();
        hbox.add(Box.createHorizontalGlue());

        okButton = new JButton("Ok");
        okButton.addActionListener(e -> onOk());
        okButton.setEnabled(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancel());

        hbox.add(okButton);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.add(cancelButton);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.setBorder(new EmptyBorder(4,10,4,8));
        return hbox;
    }

    private void onOk() {
        if (okButton.isEnabled()) {
            setVisible(false);
        }
    }
    private void onCancel() {
        choicesList.clearSelection();
        setVisible(false);
    }

    /**
     * Create a panel with a prompt, a list, and two buttons.
     * @return The panel.
     */
    private JPanel layoutComponents() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(promptLabel, BorderLayout.NORTH);
        choicesListScrollPane = new JScrollPane(choicesList);
        panel.add(choicesListScrollPane, BorderLayout.CENTER);
        choicesListScrollPane.setBorder(redBorder);
        return panel;
    }

    /**
     * Listens for list selection, and enables the OK button whenever an item is selected.
     * @param listSelectionEvent isunused.
     */
    private void listSelectionListener(@SuppressWarnings("unused") ListSelectionEvent listSelectionEvent) {
        boolean haveSelection = choicesList.getLeadSelectionIndex() >= 0;
        okButton.setEnabled(haveSelection);
        if (haveSelection) {
            getRootPane().setDefaultButton(okButton);
        }
        setListBorder();
    }

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

    private void setListBorder() {
        boolean haveFocus = choicesList.hasFocus();
        boolean haveSelection = choicesList.getLeadSelectionIndex() >= 0;
        int ix = (haveSelection?0:2) + (haveFocus?0:1);
        choicesListScrollPane.setBorder(borders[ix]);
    }

    private Color borderColor = new Color(136, 176, 220);
    private RoundedLineBorder[] borders = {
        new RoundedLineBorder(borderColor, 2, 6),
        new RoundedLineBorder(borderColor, 1, 6, 2),
        new RoundedLineBorder(Color.RED, 2, 6),
        new RoundedLineBorder(Color.RED, 1, 6, 2)
    };

    @SuppressWarnings("FieldCanBeLocal")
    private ListCellRenderer<? super String> listCellRenderer = new DefaultListCellRenderer() {
        Font normalFont = getFont();
        Font italicFont = LabelButton.fontResource(LabelButton.AVENIR).deriveFont((float)normalFont.getSize()).deriveFont(
            Font.ITALIC);

        @Override
        public Component getListCellRendererComponent(JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            // TODO: This really doesn't belong here.
            Object row = unmatchedItems.get(value.toString()).getLeft();
            Font font = (row instanceof AudioPlaylistTarget) ? italicFont : normalFont;
            // 
            Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (isSelected)
                comp.setBackground(AcmAssistantPage.bgSelectionColor);
            else
                comp.setBackground(index%2==0 ? AcmAssistantPage.bgColor : AcmAssistantPage.bgAlternateColor);
            comp.setFont(font);
            return comp;
        }
    };

    public T getSelectedItem() {
        String chosenTitle = choicesList.getSelectedValue();
        if (chosenTitle != null) {
            return unmatchedItems.get(chosenTitle);
        }
        return null;
    }
}


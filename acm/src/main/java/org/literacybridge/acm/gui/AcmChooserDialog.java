package org.literacybridge.acm.gui;

import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.redBorder;

/**
 * Dialog allows user to manually select the desired ACM.
 */
public class AcmChooserDialog extends JDialog {
    private final JList<String> choicesList;
    private final JLabel promptLabel;
    private JButton okButton;
    private JCheckBox forceSandbox;
    private JScrollPane choicesListScrollPane;

    /**
     * Constructor for the acm chooser dialog.
     * @param acmNames List of the names of the ACM. Displayed as they're given.
     * @param sandbox Initial value of the "sandbox" checkbox, displayed as "Use demo mode"
     */
    AcmChooserDialog(List<String> acmNames, boolean sandbox) {
        super((Frame)null, "Choose ACM", true);
        setLayout(new BorderLayout());
        JPanel dialogPanel = new JPanel();
        add(dialogPanel, BorderLayout.CENTER);
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        promptLabel = new JLabel("<html>Choose the ACM to open.</html>");

        choicesList = new JList<>();
        choicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        choicesList.setListData(acmNames.toArray(new String[0]));
        choicesList.addListSelectionListener(this::listSelectionListener);
        choicesList.addFocusListener(listFocusListener);
        choicesList.addMouseListener(listMouseListener);

        dialogPanel.add(setupButtons(), BorderLayout.SOUTH);
        forceSandbox.setSelected(sandbox);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(promptLabel, BorderLayout.NORTH);
        choicesListScrollPane = new JScrollPane(choicesList);
        panel.add(choicesListScrollPane, BorderLayout.CENTER);
        choicesListScrollPane.setBorder(redBorder);
        dialogPanel.add(panel);

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
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        // Center horizontally, 1/3 down (so it is easier to make it taller).
        setLocation((dim.width - this.getSize().width) / 2,
            (dim.height - this.getSize().height) / 3);
        setVisible(true);
    }

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
     * Create sandbox checkbox and the OK and Cancel buttons and their listeners.
     */
    private JComponent setupButtons() {
        Box hbox = Box.createHorizontalBox();
        forceSandbox = new JCheckBox("Use demo mode");
        hbox.add(forceSandbox);
        hbox.add(Box.createHorizontalGlue());

        okButton = new JButton("Ok");
        okButton.addActionListener(e -> onOk());
        okButton.setEnabled(false);
        hbox.add(okButton);
        hbox.add(Box.createHorizontalStrut(5));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancel());

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

    public String getSelectedItem() {
        return choicesList.getSelectedValue();
    }

    boolean getForceSandbox() {
        return forceSandbox.isSelected();
    }
}


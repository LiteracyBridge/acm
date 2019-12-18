package org.literacybridge.acm.gui.dialogs;

import org.jdesktop.swingx.JXBusyLabel;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class BusyDialog extends JDialog {
    private JLabel label;
    private boolean stopRequested = false;

    public BusyDialog(String text, Window parent) {
        this(text, parent, false);
    }

    /**
     * Dialog that displays a line of text with a spinner..
     *
     * @param text   The dialog description.
     * @param parent Parent, for positioning.
     */
    public BusyDialog(String text, Window parent, boolean addStopButton) {
        super(parent, text, ModalityType.DOCUMENT_MODAL);

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());

        JPanel borderPanel = new JPanel();
        Border outerBorder = new EmptyBorder(6, 6, 6, 6);
        Border innerBorder = new RoundedLineBorder(Color.GRAY, 1, 6, 2);
        borderPanel.setBorder(new CompoundBorder(outerBorder, innerBorder));
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setLayout(new BorderLayout());

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
        borderPanel.add(dialogPanel, BorderLayout.CENTER);
        dialogPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        int height = 120;
        this.label = new JLabel(text);
        JXBusyLabel spinner = new JXBusyLabel();
        this.label.setAlignmentX(Component.CENTER_ALIGNMENT);//.setHorizontalAlignment(SwingConstants.CENTER);
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        setResizable(false);
        setUndecorated(true);
        spinner.setBusy(true);

        dialogPanel.add(Box.createVerticalStrut(5));
        dialogPanel.add(this.label);
        dialogPanel.add(Box.createVerticalStrut(10));
        dialogPanel.add(spinner);
        if (addStopButton) {
            JButton stopButton = new JButton("Stop");
            stopButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            dialogPanel.add(Box.createVerticalStrut(10));
            dialogPanel.add(stopButton);
            stopButton.addActionListener(e -> { stopRequested = true; });
            height += 30;
        }

        setSize(new Dimension(300, height));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopRequested = true;
            }
        });
    }

    /**
     * Update the text of the dialog.
     *
     * @param text New text to display.
     */
    public void update(String text) {
        UIUtils.setLabelText(this.label, text);
    }

    public boolean isStopRequested() {
        return stopRequested;
    }
}

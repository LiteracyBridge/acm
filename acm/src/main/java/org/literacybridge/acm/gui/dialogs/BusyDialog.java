package org.literacybridge.acm.gui.dialogs;

import org.jdesktop.swingx.JXBusyLabel;
import org.literacybridge.acm.gui.util.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class BusyDialog extends JDialog {
    private JLabel label;
    private boolean stopRequested = false;

    public BusyDialog(String text, JFrame parent) {
        this(text, parent, false);
    }

    /**
     * Dialog that displays a line of text with a spinner..
     *
     * @param text   The dialog description.
     * @param parent Parent, for positioning.
     */
    public BusyDialog(String text, JFrame parent, boolean addStopButton) {
        super(parent, text, true);
        this.setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        int height = 100;
        this.label = new JLabel(text);
        JXBusyLabel spinner = new JXBusyLabel();
        this.label.setAlignmentX(Component.CENTER_ALIGNMENT);//.setHorizontalAlignment(SwingConstants.CENTER);
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        setResizable(false);
        setUndecorated(true);
        spinner.setBusy(true);

        add(Box.createVerticalStrut(5));
        add(this.label);
        add(Box.createVerticalStrut(10));
        add(spinner);
        if (addStopButton) {
            JButton stopButton = new JButton("Stop");
            stopButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(Box.createVerticalStrut(10));
            add(stopButton);
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

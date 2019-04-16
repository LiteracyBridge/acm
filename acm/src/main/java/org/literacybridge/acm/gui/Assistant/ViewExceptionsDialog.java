package org.literacybridge.acm.gui.Assistant;

import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.io.IOException;
import java.util.List;

import static org.literacybridge.acm.utils.EmailHelper.sendEmail;

/**
 * Class to show a list of exceptions to a user. This is mostly so that they will have some
 * hopefully good information to put into a trouble report.
 * <p>
 * The exceptions are shown in a tree, collapsed, so the user can just see the summary,
 * but drill in if they're curious.
 * <p>
 * A handy button is provided to email the list of exceptions to Amplio (assuming they're
 * online at the time -- no store and forward available).
 */
public class ViewExceptionsDialog extends JDialog {
    private DefaultTreeModel exceptionsTreeModel;
    private DefaultMutableTreeNode exceptionsRoot;
    private final JLabel messageLabel;

    private List<Exception> exceptions;
    private String reportHeading;
    private final JButton sendButton;
    private final JButton closeButton;

    /**
     * Constructor for the Exceptions display.
     *
     * @param owner window, for positioning this window.
     * @param title for the dialog.
     */
    public ViewExceptionsDialog(Frame owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setBackground(new Color(0xe7, 0xe7, 0xe7));

        messageLabel = new JLabel("Here they are:");
        panel.add(messageLabel, BorderLayout.NORTH);

        exceptionsRoot = new DefaultMutableTreeNode();
        JTree exceptionsTree = new JTree(exceptionsRoot);
        exceptionsTree.setBackground(new Color(0xf4, 0xf4, 0xf4));
        exceptionsTreeModel = (DefaultTreeModel) exceptionsTree.getModel();
        exceptionsTree.setRootVisible(true);
        JScrollPane exceptionsScroller = new JScrollPane(exceptionsTree);
        exceptionsScroller.setBorder(new LineBorder(new Color(0x40, 0x80, 0x40), 1));

        panel.add(exceptionsScroller, BorderLayout.CENTER);

        Box hbox = Box.createHorizontalBox();
        hbox.add(Box.createHorizontalGlue());

        sendButton = new JButton("Send to Amplio");
        sendButton.addActionListener(e -> sendTroubleReport());
        hbox.add(sendButton);
        hbox.add(Box.createHorizontalStrut(10));

        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> setVisible(false));
        hbox.add(closeButton);
        hbox.add(Box.createHorizontalStrut(5));

        panel.add(hbox, BorderLayout.SOUTH);
        add(panel);
        setSize(800, 500);
    }

    /**
     * Called to actually show the list of exceptions.
     *
     * @param message       to show to the user.
     * @param reportHeading heading if a report is sent to Amplio.
     * @param exceptions    the list of exceptions to show.
     */
    public void showExceptions(String message, String reportHeading, List<Exception> exceptions) {
        this.reportHeading = reportHeading;
        this.exceptions = exceptions;
        messageLabel.setText(message);
        for (Exception ex : exceptions) {
            ExceptionNode eNode = new ExceptionNode(ex);
            exceptionsTreeModel.insertNodeInto(eNode,
                exceptionsRoot,
                exceptionsRoot.getChildCount());
            String[] frames = ExceptionUtils.getStackFrames(ex);
            for (String frame : frames) {
                FrameNode fNode = new FrameNode(frame);
                exceptionsTreeModel.insertNodeInto(fNode, eNode, eNode.getChildCount());
            }
        }
        exceptionsTreeModel.reload();

        if (getOwner() != null) {
            setLocation(getOwner().getX() + 40, getOwner().getY() + 40);
        }
        setVisible(true);
    }

    /**
     * Format and send a trouble report email to Amplio.
     */
    private void sendTroubleReport() {
        sendButton.setEnabled(false);
        closeButton.setEnabled(false);
        StringBuilder body = new StringBuilder("Error report from content import\n");
        body.append(reportHeading);
        exceptions.forEach(ex -> body.append('\n').append(ExceptionUtils.getStackTrace(ex)));

        String from = "techsupport@amplio.org";
        String to = "techsupport@amplio.org";
        String subject = "Error report from content import";

        SwingUtilities.invokeLater(() -> {
            try {
                sendEmail(from, to, subject, body.toString(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            closeButton.setEnabled(true);
        });
    }

    /**
     * Node to hold an Exception object.
     */
    private static class ExceptionNode extends DefaultMutableTreeNode {
        ExceptionNode(Exception ex) {
            super(ex);
        }

        public String toString() {
            Exception ex = (Exception) getUserObject();
            return ex.getLocalizedMessage();
        }
    }

    /**
     * Node to hold one line from an exception stack trace.
     */
    private static class FrameNode extends DefaultMutableTreeNode {
        FrameNode(String line) {
            super(line);
        }

        public String toString() {
            return getUserObject().toString();
        }
    }

}


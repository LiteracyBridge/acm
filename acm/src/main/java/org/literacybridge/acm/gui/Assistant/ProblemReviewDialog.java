package org.literacybridge.acm.gui.Assistant;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.enumerationAsStream;
import static org.literacybridge.acm.utils.EmailHelper.sendEmail;

/**
 * Class to show a list of problems to a user. This is mostly so that they will have some
 * hopefully good information to put into a trouble report.
 * <p>
 * The problems are shown in a tree, collapsed, so the user can just see the summary,
 * but drill in if they're curious.
 * <p>
 * A handy button is provided to email the list of problems to Amplio (assuming they're
 * online at the time -- no store and forward available).
 */
public class ProblemReviewDialog extends JDialog {
    private DefaultTreeModel problemsTreeModel;
    private DefaultMutableTreeNode problemsRoot;
    private final JLabel messageLabel;

    private List<Exception> exceptions;
    private MutableTreeNode issues;
    private String reportHeading;
    private final JButton sendButton;
    private final JButton closeButton;

    /**
     * Constructor for the Exceptions display.
     *
     * @param owner window, for positioning this window.
     * @param title for the dialog.
     */
    public ProblemReviewDialog(Frame owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setBackground(new Color(0xe7, 0xe7, 0xe7));

        messageLabel = new JLabel("Here they are:");
        panel.add(messageLabel, BorderLayout.NORTH);

        problemsRoot = new DefaultMutableTreeNode();
        JTree exceptionsTree = new JTree(problemsRoot);
        exceptionsTree.setBackground(new Color(0xf4, 0xf4, 0xf4));
        problemsTreeModel = (DefaultTreeModel) exceptionsTree.getModel();
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
     * Called to actually show the list of problems.
     *
     * @param message       to show to the user.
     * @param reportHeading heading if a report is sent to Amplio.
     * @param issues        an optional Tree of issues. Only required to have a toString method.
     * @param exceptions    the list of exceptions to show.
     */
    public void showProblems(String message, String reportHeading, MutableTreeNode issues, List<Exception> exceptions) {
        this.reportHeading = reportHeading;
        this.issues = issues;
        this.exceptions = exceptions;
        DefaultMutableTreeNode exceptionsRoot = problemsRoot;
        // Generic "issues"?
        if (issues != null && (issues.getChildCount()>0 || issues.isLeaf())) {
            DefaultMutableTreeNode issuesRoot = new DefaultMutableTreeNode("Issues");
            problemsTreeModel.insertNodeInto(issuesRoot, problemsRoot,
                problemsRoot.getChildCount());
            if (issues.isLeaf()) {
                issuesRoot.add(issues);
            } else {
                while (issues.getChildCount() > 0) {
                    issuesRoot.add((MutableTreeNode) issues.getChildAt(0));
                }
            }
        }
        exceptionsRoot = new DefaultMutableTreeNode("Exceptions");
        problemsTreeModel.insertNodeInto(exceptionsRoot, problemsRoot,
            problemsRoot.getChildCount());
        messageLabel.setText(message);
        for (Exception ex : exceptions) {
            ExceptionNode eNode = new ExceptionNode(ex);
            problemsTreeModel.insertNodeInto(eNode, exceptionsRoot,
                exceptionsRoot.getChildCount());
            String[] frames = ExceptionUtils.getStackFrames(ex);
            for (String frame : frames) {
                FrameNode fNode = new FrameNode(frame);
                problemsTreeModel.insertNodeInto(fNode, eNode, eNode.getChildCount());
            }
        }
        problemsTreeModel.reload();

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
        StringBuilder body = new StringBuilder(reportHeading).append('\n');

        Enumeration en = problemsRoot.preorderEnumeration();
        int prevLevel = -1;
        int level;
        while (en.hasMoreElements()) {
            Object o = en.nextElement();
            if (o instanceof DefaultMutableTreeNode) {
                level = ((DefaultMutableTreeNode)o).getLevel();
                if (level < prevLevel) {
                    body.append('\n');
                }
                prevLevel = level;
            } else {
                level = 0;
            }
            body.append('\n').append(StringUtils.repeat("  ", level-1));
            body.append(o.toString());
        }

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


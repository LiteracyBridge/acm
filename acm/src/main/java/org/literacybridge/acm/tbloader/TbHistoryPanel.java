package org.literacybridge.acm.tbloader;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.core.spec.RecipientList.RecipientAdapter;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import static org.literacybridge.acm.gui.UIConstants.getResource;


public class TbHistoryPanel extends JPanel {
    public static final int NUM_TBS_ID = ActionEvent.ACTION_PERFORMED + 1;
    public static final int COLLECTION_DEPLOYED_ID = ActionEvent.ACTION_LAST + 2;
    public static final int COLLECTION_COLLECTED_ID = ActionEvent.ACTION_LAST + 3;
    public static final int COLLECTION_TO_COLLECT_ID = ActionEvent.ACTION_LAST + 4;
    public static final int DEPLOYMENT_DEPLOYED_ID = ActionEvent.ACTION_LAST + 5;
    public static final int DEPLOYMENT_TO_DEPLOY_ID = ActionEvent.ACTION_LAST + 6;
    public static final int FILTER_ACTION = ActionEvent.ACTION_LAST + 7;


    private static final Border grayBorder = new RoundedLineBorder(Color.lightGray, 1, 3);
    private static final Border parameterBorder = new CompoundBorder(grayBorder, new EmptyBorder(0, 2, 0, 3));
    private static final Color blueBackground = new Color(220, 235, 244); // new Color(190,230,255);
    private static final Color blueBorder = new Color(0, 157, 255);

    private final TbLoaderPanel tbLoaderPanel;

    private final TbHistory history = TbHistory.getInstance();
    private final TbHistorySummarizer summarizer = history.getSummarizer();
    private final ImageIcon unPinnedIcon;
    private final ImageIcon pinnedIcon;
    private final JButton pinButton;
    private final String unPinnedToolTip;
    private final String pinnedToolTip;
    private final ImageIcon filterIcon;
    private final JButton filterButton;

    private boolean recipientsPinned = false;
    private boolean collectMode = false;

    private JLabel numTbsLabel;
    private JLabel latestDeployedLabel; // Updated in latest deployment
    private JLabel toDeployLabel;       // Still to be updated with latest deployment
    private JLabel totalDeployedLabel;  // Updated in any deployment
    private JLabel numCollectedLabel;   // Latest operation was "collect"
    private JLabel toCollectLabel;      // Latest operation was "update"
    private Box deployedStatusBox;
    private Box collectedStatusBox;
    private Box noStatusBox;
    private Box numTbsStatusBox;
    private JLabel pathStatusLabel;
    private Box pathStatusBox;

    public TbHistoryPanel(TbLoaderPanel tbLoaderPanel) {
        this.tbLoaderPanel = tbLoaderPanel;
        unPinnedToolTip = LabelProvider.getLabel("Click to pin the selected recipients.");
        pinnedToolTip = LabelProvider.getLabel("Click to track the selected recipients.");


        setLayout(new BorderLayout());
        Box contentPanel = Box.createVerticalBox();
        contentPanel.add(makePathStatus());
        contentPanel.add(makeNumTbsStatus());
        contentPanel.add(makeDeployedStatus());
        contentPanel.add(makeCollectedStatus());
        contentPanel.add(makeNotAvailableStatus());
        contentPanel.setToolTipText("Click on any number for details about that value.");
        add(contentPanel, BorderLayout.CENTER);

        unPinnedIcon = new ImageIcon(new ImageIcon(getResource(UIConstants.ICON_UNPINNED))
            .getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        pinnedIcon = new ImageIcon(new ImageIcon(getResource(UIConstants.ICON_PINNED))
            .getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        filterIcon = new ImageIcon(new ImageIcon(getResource(UIConstants.ICON_FILTER))
            .getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));

        Box filterButtonBox = Box.createVerticalBox();
        pinButton = new JButton();
        pinButton.setIcon(unPinnedIcon);
        pinButton.setToolTipText(unPinnedToolTip);
        pinButton.addActionListener(this::onPinButtonClicked);
        pinButton.setBackground(getBackground());
        pinButton.setBorder(new RoundedLineBorder(pinButton.getBackground(), 1, 3));
        filterButtonBox.add(pinButton);

        // Custom filtering not implemented yet.
        filterButton = new JButton();
        filterButton.setIcon(filterIcon);
        filterButton.setToolTipText("Click to set up recipients filter.");
        filterButton.addActionListener(this::onFilterButtonClicked);
        filterButton.setBackground(getBackground());
        filterButton.setBorder(null);
        filterButtonBox.add(filterButton);

        filterButtonBox.add(Box.createVerticalGlue());
        filterButtonBox.setVisible(true); // Auto (or maybe OFF) is the default.

        add(filterButtonBox, BorderLayout.WEST);

        attachListeners();
    }

    /**
     * Should the "Customize" (probably the funnel) button be visible?
     *
     * @param b true if the button should be visible and enabled.
     */
    @SuppressWarnings("unused")
    public void enableCustomizeButton(boolean b) {
        filterButton.setVisible(b);
    }

    /**
     * Lets the panel know if the user is interested in TBs "to be collected" or
     * "to be deployed".
     *
     * @param collectMode True if user wants "to be collected".
     */
    public void onTbLoaderCollectModeChanged(boolean collectMode) {
        if (!recipientsPinned) {
            setCollectMode(collectMode);
        }
    }

    private void setCollectMode(boolean collectMode) {
        if (!history.haveHistory()) {
            deployedStatusBox.setVisible(false);
            collectedStatusBox.setVisible(false);
            numTbsStatusBox.setVisible(false);
            noStatusBox.setVisible(true);
        } else if (this.collectMode != collectMode) {
            this.collectMode = collectMode;
            deployedStatusBox.setVisible(!collectMode);
            collectedStatusBox.setVisible(collectMode);
            numTbsStatusBox.setVisible(true);
            noStatusBox.setVisible(false);
        }
    }

    /**
     * Lets this panel know which recipients the user is interested in.
     *
     * @param relevantRecipients a collection of RecipientAdapters.
     */
    public void setRelevantRecipients(Collection<RecipientAdapter> relevantRecipients, List<String> recipientPath) {
        if (!recipientsPinned) {
            history.setRelevantRecipients(relevantRecipients);
            setPathStatus(recipientPath);
        }
    }

    private void setPathStatus(List<String> recipientPath) {
        if (recipientPath.isEmpty()) {pathStatusLabel.setText(" All recipients");} else {
            pathStatusLabel.setText(" " + String.join(" / ", recipientPath));
        }
    }

    public void onFilterButtonClicked(ActionEvent actionEvent) {
        TbHistoryFilterDialog filterDialog = new TbHistoryFilterDialog();
        filterDialog.setVisible(true);
        if (filterDialog.isOk()) {
            setCollectMode(filterDialog.getCollectionSelected());
            setRelevantRecipients(filterDialog.getSelectedRecipients(), filterDialog.getSelectionPath());
            if (!recipientsPinned) {
                onPinButtonClicked(null);
            }
        }
    }

    private void onPinButtonClicked(ActionEvent actionEvent) {
        if (recipientsPinned) {
            recipientsPinned = false;
            pinButton.setBackground(getBackground());
            pinButton.setBorder(new RoundedLineBorder(pinButton.getBackground(), 1, 3));
            pinButton.setIcon(unPinnedIcon);
            pinButton.setToolTipText(unPinnedToolTip);
            history.setRelevantRecipients(tbLoaderPanel.getRecipientsForPartialSelection());
            setPathStatus(tbLoaderPanel.getSelectionPath());
            onTbLoaderCollectModeChanged(tbLoaderPanel.isStatsOnly());
        } else {
            recipientsPinned = true;
            pinButton.setBorder(new RoundedLineBorder(blueBorder, 1, 3)); // "sky blue"-ish
            pinButton.setBackground(blueBackground);
            pinButton.setIcon(pinnedIcon);
            pinButton.setToolTipText(pinnedToolTip);
        }
//        fireActionEvent(FILTER_ACTION, "filter");
    }

    /**
     * Attaches event listeners to the various number boxes. Also attaches the change listener
     * to the history object.
     */
    private void attachListeners() {
        numTbsLabel.addMouseListener(new MouseListener(NUM_TBS_ID, "num tbs"));
        totalDeployedLabel.addMouseListener(new MouseListener(COLLECTION_DEPLOYED_ID, "total deployed"));
        numCollectedLabel.addMouseListener(new MouseListener(COLLECTION_COLLECTED_ID, "num collected"));
        toCollectLabel.addMouseListener(new MouseListener(COLLECTION_TO_COLLECT_ID, "to collect"));
        latestDeployedLabel.addMouseListener(new MouseListener(DEPLOYMENT_DEPLOYED_ID, "num deployed"));
        toDeployLabel.addMouseListener(new MouseListener(DEPLOYMENT_TO_DEPLOY_ID, "to deploy"));

        history.addChangeListener(e -> onHistoryUpdated());
    }

    /**
     * Called when the History advertises that the history has been updated.
     */
    private void onHistoryUpdated() {
        if (history.haveHistory()) {
            numTbsLabel.setText(Integer.toString(summarizer.getNumSpecTbs()));
            totalDeployedLabel.setText(Integer.toString(summarizer.getTotalDeployed()));
            numCollectedLabel.setText(Integer.toString(summarizer.getNumCollected()));
            toCollectLabel.setText(Integer.toString(summarizer.getNumToCollect()));
            latestDeployedLabel.setText(Integer.toString(summarizer.getNumDeployedLatest()));
            toDeployLabel.setText(Integer.toString(summarizer.getNumToDeploy()));
        }
    }

    private Component makePathStatus() {
        pathStatusBox = Box.createHorizontalBox();
        pathStatusLabel = new CompactLabel(" All recipients");
        pathStatusBox.add(pathStatusLabel);
        pathStatusBox.add(Box.createHorizontalGlue());
        return pathStatusBox;
    }

    /**
     * Makes a box with the # TBs as given by the program spec.
     *
     * @return The component.
     */
    private Component makeNumTbsStatus() {
        numTbsStatusBox = Box.createHorizontalBox();
        numTbsLabel = makeBoxedLabel();
        numTbsStatusBox.add(numTbsLabel);
        numTbsStatusBox.add(new CompactLabel(" TBs (per program spec)."));
        numTbsStatusBox.add(Box.createHorizontalGlue());
        return numTbsStatusBox;
    }

    /**
     * Makes a box with "update" status: updated (with latest), to be updated (with latest).
     *
     * @return The component.
     */
    private Component makeDeployedStatus() {
        deployedStatusBox = Box.createHorizontalBox();
        latestDeployedLabel = makeBoxedLabel();
        deployedStatusBox.add(latestDeployedLabel);
        deployedStatusBox.add(new CompactLabel(" updated, "));
        toDeployLabel = makeBoxedLabel();
        toDeployLabel.setForeground(Color.BLACK);
        deployedStatusBox.add(toDeployLabel);
        deployedStatusBox.add(new CompactLabel(" to be updated."));
        deployedStatusBox.add(Box.createHorizontalGlue());
        return deployedStatusBox;
    }

    /**
     * Makes a box with "collection" status: updated, collected, to be collected.
     *
     * @return The component.
     */
    private Component makeCollectedStatus() {
        collectedStatusBox = Box.createHorizontalBox();
        totalDeployedLabel = makeBoxedLabel();
        collectedStatusBox.add(totalDeployedLabel);
        collectedStatusBox.add(new CompactLabel(" deployed, "));
        numCollectedLabel = makeBoxedLabel();
        collectedStatusBox.add(numCollectedLabel);
        collectedStatusBox.add(new CompactLabel(" collected, "));
        toCollectLabel = makeBoxedLabel();
        toCollectLabel.setForeground(Color.BLACK);
        collectedStatusBox.add(toCollectLabel);
        collectedStatusBox.add(new CompactLabel(" to be collected."));
        collectedStatusBox.add(Box.createHorizontalGlue());
        collectedStatusBox.setVisible(false); // TB-Loader starts in "Update" mode.
        return collectedStatusBox;
    }

    /**
     * Builds the box that says "Nothing available."
     *
     * @return The component.
     */
    private Component makeNotAvailableStatus() {
        noStatusBox = Box.createHorizontalBox();
        noStatusBox.add(Box.createHorizontalStrut(20));
        noStatusBox.add(new CompactLabel("<html><i>Update and collection status is not available.</i></html>"));
        noStatusBox.add(Box.createHorizontalGlue());
        noStatusBox.setVisible(false);
        return noStatusBox;
    }

    private class MouseListener extends MouseAdapter {
        int eventId;
        String command;

        public MouseListener(int eventId, String command) {
            this.eventId = eventId;
            this.command = command;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            historyActionListener(eventId, command);
        }
    }

    /**
     * If we generated ActionEvents here, this is what a listener would look like. There
     * are no external listeners, so we don't actually fire the event. If we had to, this
     * would make it easier.
     *
     * @param eventId The id of the event (count box) that we're responding to.
     * @param command Correspond textual representation of the event.
     */
    private void historyActionListener(int eventId, String command) {
        System.out.printf("History event, id: %d, command: %s\n", eventId, command);
        List<RecipientAdapter> recipients = history.getSummarizer().getRelevantRecipients();
        switch (eventId) {
            case TbHistoryPanel.NUM_TBS_ID:
            case TbHistoryPanel.COLLECTION_DEPLOYED_ID:
            case TbHistoryPanel.COLLECTION_COLLECTED_ID:
            case TbHistoryPanel.COLLECTION_TO_COLLECT_ID:
            case TbHistoryPanel.DEPLOYMENT_DEPLOYED_ID:
            case TbHistoryPanel.DEPLOYMENT_TO_DEPLOY_ID:
                TbHistoryDetails details = new TbHistoryDetails(recipients, eventId);
                details.setVisible(true);
                break;
            case TbHistoryPanel.FILTER_ACTION:
                break;
            default:
                // hmm. didn't expect this
                break;
        }
    }

    protected static JLabel makeBoxedLabel() {
        JLabel label = new CompactLabel();
        label.setOpaque(true);
        label.setBackground(Color.white);
        label.setBorder(parameterBorder);
        if (!StringUtils.isEmpty("--")) label.setText("--");
        return label;
    }

    private static class CompactLabel extends JLabel {
        private String myText;
        public CompactLabel(String text) {
            super(T(text));
            myText = text;

            Font f = getFont();
            setFont(new Font(f.getName(), f.getStyle(), 11));
            setForeground(Color.darkGray);
        }

        public CompactLabel() {
            super();
            Font f = getFont();
            setFont(new Font(f.getName(), f.getStyle(), 11));
            setForeground(Color.darkGray);
        }

        @Override
        public String getText() {
            return myText;
        }

        @Override
        public void setText(String text) {
            super.setText(T(text));
            myText = text;
        }

        private static String T(String s) {
            String result = "<html><span style='font-size:0.85em;color:green;'>" + s + "</span></html>";
            return result;
        }

    }

}

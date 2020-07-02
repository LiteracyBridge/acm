package org.literacybridge.acm.tbloader;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXDatePicker;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.TBDeviceInfo;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.FIRST_LINE_START;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.NONE;
import static java.lang.Math.max;

@SuppressWarnings("unused")
public class TbLoaderPanel extends JPanel {
    private final ProgramSpec programSpec;

    private JComboBox<String> currentLocationChooser;
    private final String[] currentLocationList = new String[] { "Select location...", "Community",
        "Jirapa office", "Wa office", "Other" };

    private JComboBox<TBDeviceInfo> driveList;

    private JLabel uploadStatus;
    private JCheckBox forceFirmware;
    private String dateRotation;

    private boolean allowForceSrn = false;
    private JComponent newPackageComponent;
    private JButton goButton;
    private JLabel firmwareVersionLabel;
    private JTextField oldFirmwareVersionText;
    private JLabel newDeploymentText;
    private JTextField oldDeploymentText;
    private JRecipientChooser recipientChooser;
    private JTextField oldCommunityText;
    private JTextField oldPackageText;
    private JXDatePicker datePicker;
    private JTextField lastUpdatedText;
    private JTextField newFirmwareVersionText;
    private Box newSrnBox;
    private JTextField oldSrnText;
    private Box newFirmwareBox;
    private JLabel nextLabel;
    private JLabel prevLabel;
    private JCheckBox forceSrn;
    private JTextField newSrnText;
    private JComboBox<String> newPackageChooser;
    private JTextField newPackageText;
    JTextArea statusCurrent;
    JTextArea statusFilename;
    JTextArea statusLog;
    private JCheckBox testDeployment;
    private JComboBox<String> actionChooser;

    private boolean allowPackageChoice;
    private void enablePackageChooser(boolean enable) { allowPackageChoice = enable;}
    private boolean usePackageChooser() {
        return allowPackageChoice;
    }

    private String[] packagesInDeployment;

    private static final String UPDATE_TB = "Update TB";
    private final String[] actionList = new String[] { UPDATE_TB, "Collect Stats" };

    private Color defaultButtonBackgroundColor;

    private final ProgressDisplayManager progressDisplayManager = new ProgressDisplayManager(this);


    public TbLoaderPanel(ProgramSpec programSpec) {
        this.programSpec = programSpec;
        layoutComponents();
    }

    public void setGridColumnWidths() {
        resizeGridColumnWidths();
    }

    public void setUploadStatus(String status) {
        uploadStatus.setVisible(status!=null);
        if (status != null) {
            uploadStatus.setText(status);
        }
    }

    public void setNewFirmwareVersion(String version) {
        newFirmwareVersionText.setText(version);
    }
    public void setOldFirmwareVersion(String version) {
        oldFirmwareVersionText.setText(version);
    }
    public void setFirmwareVersionLabel(String label) {
        firmwareVersionLabel.setText(label);
    }
    public boolean isForceFirmware() {
        return forceFirmware.isSelected();
    }

    public String getNewSrn() {
        return newSrnText.getText();
    }
    public void setNewSrn(String newSrn) {
        newSrnText.setText(newSrn);
    }
    public boolean isForceSrn() {
        return forceSrn.isSelected();
    }
    public void setForceSrn(boolean force) {
        forceSrn.setSelected(force);
    }
    public void enableForceSrn(boolean enable) {
        forceSrn.setVisible(enable);
        allowForceSrn = enable;
    }

    public boolean isTestDeployment() {
        return testDeployment.isSelected();
    }

    public void setNewDeployment(String description) {
        newDeploymentText.setText(description);
    }

    public String getDateRotation() {
        return dateRotation;
    }

    public void setSelectedRecipient(String recipientid) {
        recipientChooser.setSelectedRecipient(recipientid);
    }
    public Recipient getSelectedRecipient() {
        return recipientChooser.getSelectedRecipient();
    }

    public void setNewPackage(String newPackage) {
        if (usePackageChooser()) {
            newPackageChooser.setSelectedItem(newPackage);
        } else {
            newPackageText.setText(newPackage);
        }
    }
    public String getNewPackage() {
        if (usePackageChooser()) {
            return newPackageChooser.getSelectedItem().toString();
        } else {
            return newPackageText.getText();
        }
    }

    public void fillDriveList(List<TBDeviceInfo> roots, int selection) {
        driveList.removeAllItems();
        roots.forEach(r -> driveList.addItem(r));
        if (selection >= 0) {
            driveList.setSelectedIndex(selection);
        }
        setEnabledStates();
    }
    public TBDeviceInfo getSelectedDevice() {
        return (TBDeviceInfo)driveList.getSelectedItem();
    }

    public ProgressDisplayManager getProgressDisplayManager() {
        return progressDisplayManager;
    }

    public void fillPrevDeploymentInfo(DeploymentInfo oldDeploymentInfo) {
        if (oldDeploymentInfo != null) {
            oldSrnText.setText(oldDeploymentInfo.getSerialNumber());
            oldPackageText.setText(oldDeploymentInfo.getPackageName());
            oldDeploymentText.setText(oldDeploymentInfo.getDeploymentName());
            lastUpdatedText.setText(oldDeploymentInfo.getUpdateTimestamp());

            forceSrn.setVisible(allowForceSrn);
            forceSrn.setSelected(false);

            oldCommunityText.setText(oldDeploymentInfo.getCommunity());
            testDeployment.setSelected(false);

        } else {
            forceSrn.setVisible(false);
            forceSrn.setSelected(false);

            oldSrnText.setText("");
            oldPackageText.setText("");
            oldDeploymentText.setText("");
            forceFirmware.setSelected(false);
            oldCommunityText.setText("");
            lastUpdatedText.setText("");
        }
    }

    public void resetUi() {
        oldSrnText.setText("");
        newSrnText.setText("");
        forceSrn.setVisible(false);
        forceSrn.setSelected(false);
    }

    private Consumer<TBLoader.Operation> goListener;
    public void setGoListener(Consumer<TBLoader.Operation> goListener) {
        this.goListener = goListener;
    }

    private Consumer<Recipient> recipientListener;
    public void setRecipientListener(Consumer<Recipient> recipientListener) {
        this.recipientListener = recipientListener;
    }

    private Consumer<TBDeviceInfo> deviceListener;
    public void setDeviceListener(Consumer<TBDeviceInfo> deviceListener) {
        this.deviceListener = deviceListener;
    }

    private Consumer<Boolean> forceFirmwareListener;
    public void setForceFirmwareListener(Consumer<Boolean> forceFirmwareListener) {
        this.forceFirmwareListener = forceFirmwareListener;
    }

    private Consumer<Boolean> forceSrnListener;
    public void setForceSrnListener(Consumer<Boolean> forceSrnListener) {
        this.forceSrnListener = forceSrnListener;
    }

    private Consumer<ActionEvent> settingsListener;
    public void setSettingsListener(Consumer<ActionEvent> settingsListener) {
        this.settingsListener = settingsListener;
    }
    /**
     * Our preferred default GridBagConstraint.
     */
    GBC protoGbc;
    private void layoutComponents() {
        setBorder(new EmptyBorder(0, 10, 9, 9));
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        protoGbc = new GBC().setInsets(new Insets(0,3,0,2)).setAnchor(LINE_START).setFill(HORIZONTAL);

        int y = 0;
        layoutGreeting(y++);
        layoutUploadStatus(y++);
        layoutDeviceStatus(y++);
        layoutColumnHeadings(y++);
        layoutRecipient(y++);
        layoutDeployment(y++);
        layoutPackage(y++);
        layoutDate(y++);
        layoutFirmware(y++);
        layoutSerialNumber(y++);
        layoutGoButton(y++);
        layoutStatus(y++);

        resizeGridColumnWidths();
//        currentLocationChooser.doLayout();
    }

    /**
     * Sets the widths of columns 1 & 2 in the grid. Examines all components in those two
     * columns, finding the largest minimum width. It then sets the minimum width of the next/
     * prev labels to that width, thereby setting the two columns' minimum widths to the same
     * value. That, in turn, causes the two columns to be the same width, and to resize together.
     */
    private JComponent[] columnComponents;

    private void resizeGridColumnWidths() {
        if (columnComponents == null) {
            columnComponents = new JComponent[] {
                // This list should contain all components in columns 1 & 2, though not those
                // spanning multiple columns.
                // This list need not contain prevLabel or nextLabel; we assume that those two
                // are "small-ish", and won't actually provide the maximimum minimum width.
                driveList, currentLocationChooser, newDeploymentText, oldDeploymentText,
                recipientChooser, oldCommunityText, newPackageComponent, oldPackageText, datePicker,
                lastUpdatedText, newFirmwareVersionText, oldFirmwareVersionText, newSrnBox,
                oldSrnText, newFirmwareBox};
        }
        int maxMinWidth = 0;
        for (JComponent c : columnComponents) {
            if (c != null) maxMinWidth = max(maxMinWidth, c.getMinimumSize().width);
        }
        Dimension d = nextLabel.getMinimumSize();
        d.width = maxMinWidth;
        nextLabel.setMinimumSize(d);
        prevLabel.setMinimumSize(d);
    }

    private void layoutGreeting(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setInsets(new Insets(0, 0, 0, 0))
            .setGridwidth(3);

        JPanel outerGreetingBox = new JPanel();
        outerGreetingBox.setLayout(new BorderLayout());

        Authenticator authInstance = Authenticator.getInstance();
        Box greetingBox = Box.createHorizontalBox();
        boolean isBorrowed = Authenticator.getInstance().getTbSrnHelper().isBorrowedId();
        String deviceIdHex = "000C";
        String greetingString = String.format("<html><nobr>Hello <b>%s</b>! <i><span style='font-size:0.85em;color:gray'>(%sTB-Loader ID: %s)</span></i></nobr></html>",
                authInstance.getName(),
            isBorrowed?"Using ":"", deviceIdHex);
        JLabel greeting = new JLabel(greetingString);
        greetingBox.add(greeting);
        greetingBox.add(Box.createHorizontalStrut(10));

        // Select "Community", "LBG Office", "Other"
        JLabel currentLocationLabel = new JLabel("Updating from:");
        currentLocationChooser = new JComboBox<>(currentLocationList);
        currentLocationChooser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        // If ever we don't set the selection, we should add the red border.
        //currentLocationChooser.setBorder(new LineBorder(Color.RED, 1, true));
        currentLocationChooser.setSelectedIndex(currentLocationChooser.getItemCount() - 1);
        boolean useLocationChooser = false;
        //noinspection ConstantConditions
        if (useLocationChooser) {
            currentLocationChooser.addActionListener(e -> {
                Border border;
                if (currentLocationChooser.getSelectedIndex() == 0) {
                    border = new LineBorder(Color.RED, 1, true);
                } else {
                    border = new LineBorder(new Color(0, 0, 0, 0), 1, true);
                }
                currentLocationChooser.setBorder(border);
                setEnabledStates();
            });
            greetingBox.add(currentLocationLabel);
            greetingBox.add(Box.createHorizontalStrut(10));
            greetingBox.add(currentLocationChooser);
        }

        outerGreetingBox.add(greetingBox, BorderLayout.WEST);

        ImageIcon gearImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_GEAR_32_PX));
        LabelButton configureButton = new LabelButton(gearImageIcon);
        configureButton.setIcon(gearImageIcon);
        configureButton.setToolTipText("Settings");
        configureButton.setMaximumSize(new Dimension(20,16));
        configureButton.addActionListener(e->{if (settingsListener!=null) settingsListener.accept(e);});
        outerGreetingBox.add(configureButton, BorderLayout.EAST);
        
        add(outerGreetingBox, gbc);
    }

    private void layoutUploadStatus(int y) {
        // Upload status.
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setGridwidth(3)
            .setGridx(0);

        uploadStatus = new JLabel();
        uploadStatus.setVisible(false);

        add(uploadStatus, gbc);
    }

    private void layoutDeviceStatus(int y) {
        // TB Drive letter / volume label.
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setGridwidth(3)
            .setGridx(0);

        // Windows drive letter and volume name.
        Box deviceBox = Box.createHorizontalBox();
        JLabel deviceLabel = new JLabel("Talking Book Device:");
        deviceBox.add(deviceLabel);
        deviceBox.add(Box.createHorizontalStrut(10));
        driveList = new JComboBox<>();
        deviceBox.add(driveList);
        deviceBox.add(Box.createHorizontalStrut(10));
        driveList.addItemListener(this::onTbDeviceSelected);

        testDeployment = new JCheckBox();
        testDeployment.setText("Deploying today for testing only.");
        testDeployment.setSelected(false);
        testDeployment.setToolTipText(
            "Check if only testing the Deployment. Uncheck if sending the Deployment out to the field.");
        int checkboxHeight = new JLabel().getMinimumSize().height;
        testDeployment.setMinimumSize(new Dimension(testDeployment.getMinimumSize().width,
            checkboxHeight));
        deviceBox.add(testDeployment);

        deviceBox.add(Box.createHorizontalGlue());

        add(deviceBox, gbc);

    }

    private void layoutColumnHeadings(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setWeightx(1.0);

        // Headings for "Next", "Previous"
        nextLabel = new JLabel("Next");
        prevLabel = new JLabel("Previous");

        add(nextLabel, gbc.withGridx(1));
        add(prevLabel, gbc);
    }

    private void layoutRecipient(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setAnchor(FIRST_LINE_START);

        JLabel communityLabel = new JLabel("Community:");
        recipientChooser = new JRecipientChooser();
        recipientChooser.populate(programSpec);
        recipientChooser.addActionListener(this::onCommunitySelected);
        oldCommunityText = new JTextField();
        oldCommunityText.setEditable(false);

        // Recipient Chooser.
        add(communityLabel, gbc.withGridx(0));
        add(recipientChooser, gbc);
        add(oldCommunityText, gbc);
    }

    private void layoutDeployment(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setAnchor(FIRST_LINE_START);

        // Deployment name / version.
        JLabel deploymentLabel = new JLabel("Deployment:");
        newDeploymentText = new JLabel();
//        newDeploymentText.setEditable(false);
        oldDeploymentText = new JTextField();
        oldDeploymentText.setEditable(false);

        newDeploymentText.setOpaque(true);
        newDeploymentText.setBackground(oldDeploymentText.getBackground());
        newDeploymentText.setBorder(oldDeploymentText.getBorder());

        add(deploymentLabel, gbc.withGridx(0));
        add(newDeploymentText, gbc);
        add(oldDeploymentText, gbc);
    }

    private void layoutPackage(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y);

        // Package (aka 'Content', aka 'image')
        JLabel contentPackageLabel = new JLabel("Content Package:");
        if (usePackageChooser()) {
            newPackageChooser = new JComboBox<>(packagesInDeployment);
            newPackageComponent = newPackageChooser;
        } else {
            newPackageText = new JTextField();
            newPackageText.setEditable(false);
            newPackageComponent = newPackageText;
        }
        oldPackageText = new JTextField();
        oldPackageText.setEditable(false);

        add(contentPackageLabel, gbc.withGridx(0));
        add(newPackageComponent, gbc);
        add(oldPackageText, gbc);
    }

    private void layoutDate(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y);

        // Deployment date.
        // Select "First Rotation Date", default today.
        JLabel dateLabel = new JLabel("First Rotation Date:");
        datePicker = new JXDatePicker(new Date());
        dateRotation = datePicker.getDate().toString();
        datePicker.getEditor().setEditable(false);
        datePicker.setFormats("yyyy/MM/dd"); //dd MMM yyyy
        datePicker.addActionListener(e -> dateRotation = datePicker.getDate().toString());
        lastUpdatedText = new JTextField();
        lastUpdatedText.setEditable(false);

        add(dateLabel, gbc.withGridx(0));
        add(datePicker, gbc);
        add(lastUpdatedText, gbc);

    }

    private void layoutFirmware(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y);

        // Show firmware version.
        forceFirmware = new JCheckBox();
        forceFirmware.setText("Force refresh");
        forceFirmware.setSelected(false);
        forceFirmware.setToolTipText(
            "Check to force a re-flash of the firmware. This should almost never be needed.");
        forceFirmware.addActionListener((e) -> {
            if (forceFirmwareListener != null)
                forceFirmwareListener.accept(forceFirmware.isSelected());
        });
        int checkboxHeight = new JLabel().getMinimumSize().height;
        forceFirmware.setMinimumSize(new Dimension(forceFirmware.getMinimumSize().width,
            checkboxHeight));

        firmwareVersionLabel = new JLabel("Firmware:");
        newFirmwareVersionText = new JTextField();
        newFirmwareVersionText.setEditable(false);
        newFirmwareBox = Box.createHorizontalBox();
        newFirmwareBox.add(newFirmwareVersionText);
        newFirmwareBox.add(Box.createHorizontalStrut(10));
        newFirmwareBox.add(forceFirmware);

        oldFirmwareVersionText = new JTextField();
        oldFirmwareVersionText.setEditable(false);

        add(firmwareVersionLabel, gbc.withGridx(0));
        add(newFirmwareBox, gbc);
        add(oldFirmwareVersionText, gbc);
    }

    private void layoutSerialNumber(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y);

        forceSrn = new JCheckBox();
        forceSrn.setText("Replace");
        forceSrn.setSelected(false);
        forceSrn.setToolTipText("Check to force a new Serial Number. DO NOT USE THIS unless "
            + "you have a good reason to believe that this SRN has been "
            + "duplicated to multiple Talking Books. This should be exceedingly rare.");
        forceSrn.addActionListener(this::forceSrnListener);
        forceSrn.setVisible(allowForceSrn);

        // Show serial number.
        JLabel srnLabel = new JLabel("Serial number:");
        newSrnText = new JTextField();
        newSrnText.setEditable(false);
        newSrnBox = Box.createHorizontalBox();
        newSrnBox.add(newSrnText);
        newSrnBox.add(Box.createHorizontalStrut(10));
        newSrnBox.add(forceSrn);

        oldSrnText = new JTextField();
        oldSrnText.setEditable(false);

        add(srnLabel, gbc.withGridx(0));
        add(newSrnBox, gbc);
        add(oldSrnText, gbc);
    }

    private void layoutGoButton(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setFill(NONE)
            .setAnchor(CENTER)
            .setGridwidth(3);

        actionChooser = new JComboBox<>(actionList);
        goButton = new JButton("Go!");
        defaultButtonBackgroundColor = goButton.getBackground();
        goButton.setEnabled(false);
        goButton.setForeground(Color.GRAY);
        goButton.setOpaque(true);
        goButton.addActionListener(this::onGoButton);
        Box actionBox = Box.createHorizontalBox();
        actionBox.add(Box.createHorizontalGlue());
        actionBox.add(actionChooser);
        actionBox.add(goButton);
        actionBox.add(Box.createHorizontalGlue());

        add(actionBox, gbc);
    }

    private void layoutStatus(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridx(0)
            .setGridwidth(3);

        statusCurrent = new JTextArea(1, 80);
        statusCurrent.setEditable(false);
        statusCurrent.setLineWrap(true);

        statusFilename = new JTextArea(1, 80);
        statusFilename.setEditable(false);
        statusFilename.setLineWrap(false);
        statusFilename.setFont(new Font("Sans-Serif", Font.PLAIN, 10));

        statusLog = new JTextArea(2, 80);
        statusLog.setEditable(false);
        statusLog.setLineWrap(true);

        JScrollPane statusScroller = new JScrollPane(statusLog);
        statusScroller.setBorder(null); // eliminate black border around status log
        statusScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Status display
        add(statusCurrent, gbc.withGridy(y));
        add(statusFilename, gbc);
        add(statusScroller, gbc.withWeighty(1).setFill(BOTH));
    }

    private void onTbDeviceSelected(ItemEvent e) {
        // Is this a new value?
        if (e.getStateChange() != ItemEvent.SELECTED) return;
        if (deviceListener != null) {
            deviceListener.accept((TBDeviceInfo)driveList.getSelectedItem());
        }
        setEnabledStates();
    }

    private void onCommunitySelected(ActionEvent actionEvent) {
        Recipient recipient = recipientChooser.getSelectedRecipient();
        if (recipientListener != null) {
            recipientListener.accept(recipient);
        }
        setEnabledStates();
    }

    /**
     * If user checks the option to allocate a new SRN, this will prompt them to consider their
     * choice. Switches the display between the old SRN and "-- to be assigned --".
     *
     * @param actionEvent is unused.
     */
    private void forceSrnListener(ActionEvent actionEvent) {
        if (forceSrnListener != null) {
            forceSrnListener.accept(forceSrn.isSelected());
        }
    }


    private void onGoButton(ActionEvent actionEvent) {
        TBLoader.Operation operation;
        boolean isUpdate = actionChooser.getSelectedItem()
            .toString()
            .equalsIgnoreCase(UPDATE_TB);
        operation = isUpdate ? TBLoader.Operation.Update : TBLoader.Operation.CollectStats;
        goListener.accept(operation);
    }

    private void setEnabledStates() {
        // Must have device. Update must not be in progress.
        boolean enabled = isDriveConnected();
        // Must have set location.
        enabled = enabled && (currentLocationChooser.getSelectedIndex() != 0);
        // Must have community.
        enabled = enabled && (getSelectedRecipient() != null);
        goButton.setEnabled(enabled);
        goButton.setBackground(enabled ? Color.GREEN : defaultButtonBackgroundColor);
        goButton.setForeground(enabled ? new Color(0, 192, 0) : Color.GRAY);
    }

    private synchronized boolean isDriveConnected() {
        boolean connected = false;
        TbFile drive;

        if (driveList.getItemCount() > 0) {
            drive = ((TBDeviceInfo) driveList.getSelectedItem()).getRootFile();
            if (drive != null) connected = true;
        }
        return connected;
    }

}

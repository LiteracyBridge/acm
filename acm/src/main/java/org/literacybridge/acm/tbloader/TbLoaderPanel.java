package org.literacybridge.acm.tbloader;

import org.jdesktop.swingx.JXDatePicker;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbloader.TBDeviceInfo;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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

    private JPanel contentPanel;

    public static class Builder {
        private ProgramSpec programSpec;
        private String[] packagesInDeployment;
        private Consumer<ActionEvent> settingsIconClickedListener;
        private Consumer<TBLoader.Operation> goListener;
        private Consumer<Recipient> recipientListener;
        private Consumer<TBDeviceInfo> deviceListener;
        private Consumer<Boolean> forceFirmwareListener;
        private Consumer<Boolean> forceSrnListener;
        private TBLoader.TB_ID_STRATEGY tbIdStrategy;
        private boolean allowPackageChoice;

        public Builder withProgramSpec(ProgramSpec programSpec) {this.programSpec = programSpec; return this;}
        public Builder withPackagesInDeployment(String[] packagesInDeployment) {this.packagesInDeployment = packagesInDeployment; return this;}
        public Builder withSettingsClickedListener(Consumer<ActionEvent> settingsIconClickedListener) {this.settingsIconClickedListener = settingsIconClickedListener; return this;}
        public Builder withGoListener(Consumer<TBLoader.Operation> goListener) {this.goListener = goListener; return this;}
        public Builder withRecipientListener(Consumer<Recipient> recipientListener) {this.recipientListener = recipientListener; return this;}
        public Builder withDeviceListener(Consumer<TBDeviceInfo> deviceListener) {this.deviceListener = deviceListener; return this;}
        public Builder withForceFirmwareListener(Consumer<Boolean> forceFirmwareListener) {this.forceFirmwareListener = forceFirmwareListener; return this;}
        public Builder withForceSrnListener(Consumer<Boolean> forceSrnListener) {this.forceSrnListener = forceSrnListener; return this;}

        public Builder withTbIdStrategy(TBLoader.TB_ID_STRATEGY tbIdStrategy) {this.tbIdStrategy = tbIdStrategy; return this;}
        public Builder withAllowPackageChoice(boolean allowPackageChoice) {this.allowPackageChoice = allowPackageChoice; return this;}
        
        public TbLoaderPanel build() {
            return new TbLoaderPanel(this);
        }
    }

    private final ProgramSpec programSpec;
    private final String[] packagesInDeployment;

    private JComboBox<String> currentLocationChooser;
    private final String[] currentLocationList = new String[] { "Select location...", "Community",
        "Jirapa office", "Wa office", "Other" };

    private JComboBox<TBDeviceInfo> driveList;

    private JLabel uploadStatus;
    private JCheckBox forceFirmware;
    private String dateRotation;

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
    
    private JCheckBox forceTbId;
    private JTextField newTbIdText;

    // New package display and/or choice (depending on "allowPackageChoice" setting)
    private CardLayout newPackageLayout;            // To switch between the two different new package components.
    private JPanel newPackageContainer;             // Container for the "newPackage" component. One at a time is "active".
    private JComboBox<String> newPackageChooser;    // A combo box to choose the package.
    private JTextField newPackageText;              // A text field that displays the package.

    JTextArea statusCurrent;
    JTextArea statusFilename;
    JTextArea statusLog;
    private JCheckBox testDeployment;
    private JComboBox<String> actionChooser;

    private TBLoader.TB_ID_STRATEGY tbIdStrategy;
    private boolean allowPackageChoice;

    private static final String UPDATE_TB = "Update TB";
    private final String[] actionList = new String[] { UPDATE_TB, "Collect Stats" };

    private Color defaultButtonBackgroundColor;

    private final ProgressDisplayManager progressDisplayManager = new ProgressDisplayManager(this);

    public TbLoaderPanel(Builder builder) {
        this.programSpec = builder.programSpec;
        this.packagesInDeployment = builder.packagesInDeployment;
        this.settingsIconClickedListener = builder.settingsIconClickedListener;
        this.goListener = builder.goListener;
        this.recipientListener = builder.recipientListener;
        this.deviceListener = builder.deviceListener;
        this.forceFirmwareListener = builder.forceFirmwareListener;
        this.forceSrnListener = builder.forceSrnListener;
        this.tbIdStrategy = builder.tbIdStrategy;
        this.allowPackageChoice = builder.allowPackageChoice;

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

    void enablePackageChoice(boolean allowPackageChoice) {
        if (allowPackageChoice != this.allowPackageChoice) {
            String newPackageSelected = getNewPackage();
            this.allowPackageChoice = allowPackageChoice;
            setNewPackage(newPackageSelected);
            showNewPackage();
        }
    }
    private void showNewPackage() {
        newPackageLayout.show(newPackageContainer, allowPackageChoice ? "CHOICE" : "NOCHOICE");
    }


    // Talking Book ID
    public String getNewSrn() {
        return newTbIdText.getText();
    }
    public void setNewSrn(String newSrn) {
        newTbIdText.setText(newSrn);
    }
    public boolean getForceTbId() {
        return forceTbId.isSelected();
    }
    public void setForceTbId(boolean force) {
        forceTbId.setSelected(force);
    }
    void setTbIdStrategy(TBLoader.TB_ID_STRATEGY tbIdStrategy) {
        this.tbIdStrategy = tbIdStrategy;
        forceTbId.setVisible(tbIdStrategy.allowsManual());
    }

    public boolean isTestDeployment() {
        return testDeployment.isSelected();
    }
    public void setTestDeployment(boolean isTestDeployment) {
        testDeployment.setSelected(isTestDeployment);
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
        // Set both; user may switch between views.
        newPackageChooser.setSelectedItem(newPackage);
        newPackageText.setText(newPackage);
    }
    public String getNewPackage() {
        if (allowPackageChoice) {
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

            forceTbId.setVisible(tbIdStrategy.allowsManual());
            forceTbId.setSelected(false);

            oldCommunityText.setText(oldDeploymentInfo.getCommunity());
            testDeployment.setSelected(false);

        } else {
            forceTbId.setVisible(false);
            forceTbId.setSelected(false);

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
        newTbIdText.setText("");
        forceTbId.setVisible(false);
        forceTbId.setSelected(false);
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

    // A listener for clicks on the settings icon.
    private Consumer<ActionEvent> settingsIconClickedListener;

    /**
     * Allows our caller to receive notifications that the settings icon was clicked.
     * @param settingsIconClickedListener A consumer to be called when the settings icon is clicked.
     */
    public void setSettingsIconClickedListener(Consumer<ActionEvent> settingsIconClickedListener) {
        this.settingsIconClickedListener = settingsIconClickedListener;
    }
    /**
     * Our preferred default GridBagConstraint.
     */
    GBC protoGbc;
    private void layoutComponents() {
        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());
        contentPanel = new JPanel();
        Border outerBorder = new EmptyBorder(12, 12, 12, 12);
        Border innerBorder = new RoundedLineBorder(Color.GRAY, 1, 6, 2);
        contentPanel.setBorder(new CompoundBorder(outerBorder, innerBorder));
        add(contentPanel, BorderLayout.CENTER);

        GridBagLayout layout = new GridBagLayout();
        contentPanel.setLayout(layout);

        protoGbc = new GBC().setInsets(new Insets(0,3,1,2)).setAnchor(LINE_START).setFill(HORIZONTAL);

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
                recipientChooser, oldCommunityText, newPackageContainer, oldPackageText, datePicker,
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
                authInstance.getUserName(),
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
        configureButton.addActionListener(e->{if (settingsIconClickedListener !=null) settingsIconClickedListener.accept(e);});
        outerGreetingBox.add(configureButton, BorderLayout.EAST);

        contentPanel.add(outerGreetingBox, gbc);
    }

    private void layoutUploadStatus(int y) {
        // Upload status.
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setGridwidth(3)
            .setGridx(0);

        uploadStatus = new JLabel();
        uploadStatus.setVisible(false);

        contentPanel.add(uploadStatus, gbc);
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

        contentPanel.add(deviceBox, gbc);

    }

    private void layoutColumnHeadings(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y)
            .setWeightx(1.0);

        // Headings for "Next", "Previous"
        nextLabel = new JLabel("Next");
        prevLabel = new JLabel("Previous");

        contentPanel.add(nextLabel, gbc.withGridx(1));
        contentPanel.add(prevLabel, gbc);
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
        contentPanel.add(communityLabel, gbc.withGridx(0));
        contentPanel.add(recipientChooser, gbc);
        contentPanel.add(oldCommunityText, gbc);
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

        contentPanel.add(deploymentLabel, gbc.withGridx(0));
        contentPanel.add(newDeploymentText, gbc);
        contentPanel.add(oldDeploymentText, gbc);
    }

    private void layoutPackage(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y);

        // Package (aka 'Content', aka 'image')
        JLabel contentPackageLabel = new JLabel("Content Package:");
        // For when 'allowPackageChoice' is true
        newPackageChooser = new JComboBox<>(packagesInDeployment);
        // For when it is false
        newPackageText = new JTextField();
        newPackageText.setEditable(false);

        newPackageLayout = new CardLayout();
        newPackageContainer = new JPanel(newPackageLayout);
        newPackageContainer.add(newPackageChooser, "CHOICE");
        newPackageContainer.add(newPackageText, "NOCHOICE");
        showNewPackage();

        oldPackageText = new JTextField();
        oldPackageText.setEditable(false);

        contentPanel.add(contentPackageLabel, gbc.withGridx(0));
        contentPanel.add(newPackageContainer, gbc);
        contentPanel.add(oldPackageText, gbc);
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

        contentPanel.add(dateLabel, gbc.withGridx(0));
        contentPanel.add(datePicker, gbc);
        contentPanel.add(lastUpdatedText, gbc);

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

        contentPanel.add(firmwareVersionLabel, gbc.withGridx(0));
        contentPanel.add(newFirmwareBox, gbc);
        contentPanel.add(oldFirmwareVersionText, gbc);
    }

    private void layoutSerialNumber(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridy(y);

        forceTbId = new JCheckBox();
        forceTbId.setText("Replace");
        forceTbId.setSelected(false);
        forceTbId.setToolTipText("Check to force a new Talking Book ID. DO NOT USE THIS unless "
            + "you have a good reason to believe that this ID has been "
            + "duplicated to multiple Talking Books. This should be exceedingly rare.");
        forceTbId.addActionListener(this::forceSrnListener);
        forceTbId.setVisible(tbIdStrategy.allowsManual());

        // Show serial number.
        JLabel srnLabel = new JLabel("Serial number:");
        newTbIdText = new JTextField();
        newTbIdText.setEditable(false);
        newSrnBox = Box.createHorizontalBox();
        newSrnBox.add(newTbIdText);
        newSrnBox.add(Box.createHorizontalStrut(10));
        newSrnBox.add(forceTbId);

        oldSrnText = new JTextField();
        oldSrnText.setEditable(false);

        contentPanel.add(srnLabel, gbc.withGridx(0));
        contentPanel.add(newSrnBox, gbc);
        contentPanel.add(oldSrnText, gbc);
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

        contentPanel.add(actionBox, gbc);
    }

    private void layoutStatus(int y) {
        GBC gbc = new GBC(protoGbc)
            .setGridx(0)
            .setGridwidth(3);

        statusCurrent = new JTextArea(1, 80);
        statusCurrent.setEditable(false);
        statusCurrent.setLineWrap(true);
        statusCurrent.setBorder(new LineBorder(Color.LIGHT_GRAY));

        statusFilename = new JTextArea(1, 80);
        statusFilename.setEditable(false);
        statusFilename.setLineWrap(false);
        statusFilename.setFont(new Font("Sans-Serif", Font.PLAIN, 10));
        statusFilename.setBorder(new LineBorder(Color.LIGHT_GRAY));

        statusLog = new JTextArea(2, 80);
        statusLog.setEditable(false);
        statusLog.setLineWrap(true);
        statusLog.setBorder(new LineBorder(Color.LIGHT_GRAY));

        JScrollPane statusScroller = new JScrollPane(statusLog);
        statusScroller.setBorder(null); // eliminate black border around status log
        statusScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Status display
        contentPanel.add(statusCurrent, gbc.withGridy(y));
        contentPanel.add(statusFilename, gbc);
        contentPanel.add(statusScroller, gbc.withWeighty(1).setFill(BOTH));
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
            forceSrnListener.accept(forceTbId.isSelected());
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

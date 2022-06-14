package org.literacybridge.acm.tbloader;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXDatePicker;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.device.DeviceInfo;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.acm.tbloader.TBLoader.TbDeviceInfoHolder;
import org.literacybridge.core.fs.TbFile;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.spec.Recipient;
import org.literacybridge.core.tbloader.DeploymentInfo;
import org.literacybridge.core.tbdevice.TbDeviceInfo;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.FIRST_LINE_START;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.NONE;
import static java.lang.Math.max;
import static org.literacybridge.acm.Constants.ACM_VERSION;
import static org.literacybridge.acm.tbbuilder.TBBuilder.MAX_PACKAGES_TBV1;
import static org.literacybridge.acm.tbbuilder.TBBuilder.MAX_PACKAGES_TBV2;
import static org.literacybridge.acm.utils.SwingUtils.getApplicationRelativeLocation;

@SuppressWarnings("unused")
public class TbLoaderPanel extends JPanel {

    private JPanel contentPanel;
    private Box updateTb2FirmwareBox;
    private JButton updateTb2FirmwareButton;
    private JComboBox<String> deviceVersionBox;

    public static class Builder {
        private ProgramSpec programSpec;
        private String[] packagesInDeployment;
        private Map<String, String> packageNameMap;
        private Consumer<ActionEvent> settingsIconClickedListener;
        private Consumer<TBLoader.Operation> goListener;
        private Consumer<Recipient> recipientListener;
        private Consumer<TbDeviceInfo> deviceSelectedListener;
        private Consumer<TbDeviceInfo.DEVICE_VERSION> deviceVersionSelectedListener;
        private Consumer<Boolean> forceFirmwareListener;
        private Consumer<Boolean> forceSrnListener;
        private Supplier<Boolean> updateTb2FirmwareListener;
        private TBLoader.TB_ID_STRATEGY tbIdStrategy;
        private boolean allowPackageChoice;
        private TBLoader.TbLoaderConfig tbLoaderConfig;

        public Builder withProgramSpec(ProgramSpec programSpec) {this.programSpec = programSpec; return this;}
        public Builder withPackagesInDeployment(String[] packagesInDeployment) {this.packagesInDeployment = packagesInDeployment; return this;}
        public Builder withPackageNameMap(Map<String, String> packageNameMap) {this.packageNameMap = packageNameMap; return this;}
        public Builder withSettingsClickedListener(Consumer<ActionEvent> settingsIconClickedListener) {this.settingsIconClickedListener = settingsIconClickedListener; return this;}
        public Builder withGoListener(Consumer<TBLoader.Operation> goListener) {this.goListener = goListener; return this;}
        public Builder withRecipientListener(Consumer<Recipient> recipientListener) {this.recipientListener = recipientListener; return this;}
        public Builder withDeviceSelectedListener(Consumer<TbDeviceInfo> deviceSelectedListener) {this.deviceSelectedListener = deviceSelectedListener; return this;}
        public Builder withDeviceVersionSelectedListener(Consumer<TbDeviceInfo.DEVICE_VERSION> deviceVersionSelectedListener) {this.deviceVersionSelectedListener = deviceVersionSelectedListener; return this;}
        public Builder withForceFirmwareListener(Consumer<Boolean> forceFirmwareListener) {this.forceFirmwareListener = forceFirmwareListener; return this;}
        public Builder withForceSrnListener(Consumer<Boolean> forceSrnListener) {this.forceSrnListener = forceSrnListener; return this;}
        public Builder withUpdateTb2FirmwareListener(Supplier<Boolean> updateTb2FirmwareListener) {this.updateTb2FirmwareListener = updateTb2FirmwareListener; return this;}

        public Builder withTbIdStrategy(TBLoader.TB_ID_STRATEGY tbIdStrategy) {this.tbIdStrategy = tbIdStrategy; return this;}
        public Builder withAllowPackageChoice(boolean allowPackageChoice) {this.allowPackageChoice = allowPackageChoice; return this;}
        public Builder withTbLoaderConfig(TBLoader.TbLoaderConfig tbLoaderConfig) {this.tbLoaderConfig=tbLoaderConfig; return this;}

        public TbLoaderPanel build() {
            return new TbLoaderPanel(this);
        }
    }

    private final Builder builder;

    private final ProgramSpec programSpec;
    private final String[] packagesInDeployment;
    private final Map<String, String> packageNameMap;

    private JComboBox<String> currentLocationChooser;
    private final String[] currentLocationList = new String[] { "Select location...", "Community",
        "Jirapa office", "Wa office", "Other" };

    private JComboBox<TbDeviceInfoHolder> driveList;

    private JLabel uploadStatus;
    private JCheckBox forceFirmware;
    private String dateRotation;

    private JButton goButton;
    private JLabel firmwareVersionLabel;
    private JTextField oldFirmwareVersionText;
    private JLabel newDeploymentText;
    private JTextField oldDeploymentText;

    private JRecipientChooser recipientChooser;
    // Old recipient display uses "chooser" if within the program (so we have program sepc), or
    // text area with data from the deployment.properties if not within the program.
    private JRecipientChooser oldRecipientChooser;
    private JTextArea oldRecipientText;
    private CardLayout oldRecipientLayout;
    private JPanel oldRecipientContainer;

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
    private JTextField newPackageChosen;           // Displays the currently chosen package(s).
    private JTextField newPackageText;              // A text field that displays the package.
    // One or more currently selected packages.
    private List<String> selectedPackages = new ArrayList<>();
    private final List<String> defaultPackages = new ArrayList<>();

    JTextArea statusCurrent;
    JTextArea statusFilename;
    JTextArea statusLog;
    private JCheckBox testDeployment;
    private JComboBox<String> actionChooser;

    private TBLoader.TB_ID_STRATEGY tbIdStrategy;
    private boolean isRememberPackageSelection = false;
    private boolean allowPackageChoice;
    private final TBLoader.TbLoaderConfig tbLoaderConfig;

    private static final String UPDATE_TB = "Update TB";
    private final String[] actionList = new String[] { UPDATE_TB, "Collect Stats" };

    private Color defaultButtonBackgroundColor;
    private Color defaultButtonForegroundColor;

    private final ProgressDisplayManager progressDisplayManager = new ProgressDisplayManager(this);

    public TbLoaderPanel(Builder builder) {
        this.builder = builder;
        this.programSpec = builder.programSpec;
        this.packagesInDeployment = builder.packagesInDeployment;
        this.packageNameMap = builder.packageNameMap;
        this.settingsIconClickedListener = builder.settingsIconClickedListener;
        this.goListener = builder.goListener;
        this.recipientListener = builder.recipientListener;
        this.deviceSelectedListener = builder.deviceSelectedListener;
        this.deviceVersionSelectedListener = builder.deviceVersionSelectedListener;
        this.forceFirmwareListener = builder.forceFirmwareListener;
        this.forceSrnListener = builder.forceSrnListener;
        this.tbIdStrategy = builder.tbIdStrategy;
        this.allowPackageChoice = builder.allowPackageChoice;
        this.tbLoaderConfig = builder.tbLoaderConfig;

        layoutComponents();
    }

    @Override
    public void setEnabled(boolean enabled) {
        boolean isEnabled = isEnabled();
        super.setEnabled(enabled);
        if (enabled != isEnabled) {
            setEnabledStates();
        }
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
        checkV2FirmwareUpdateRequired();
    }
    public void setOldFirmwareVersion(String version) {
        oldFirmwareVersionText.setText(version);
        checkV2FirmwareUpdateRequired();
    }
    public void setFirmwareVersionLabel(String label) {
        firmwareVersionLabel.setText(label);
    }
    public boolean isForceFirmware() {
        return forceFirmware.isSelected();
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

    public void refresh() {
        setEnabledStates();
    }

    void enablePackageChoice(boolean allowPackageChoice) {
        if (allowPackageChoice != this.allowPackageChoice) {
            if (allowPackageChoice) {
                newPackageChosen.setText(String.join(",", selectedPackages));
            } else {
                selectedPackages.clear();
                selectedPackages.addAll(defaultPackages);
                newPackageText.setText(String.join(",", defaultPackages));
            }
            this.allowPackageChoice = allowPackageChoice;
            showNewPackage();
        }
    }
    private void showNewPackage() {
        // Shows wither a package chooser or a package name.
        newPackageLayout.show(newPackageContainer, allowPackageChoice ? "CHOICE" : "NOCHOICE");
    }

    public void setDefaultPackage(String defaultPackage) {
        List<String> pkg = StringUtils.isNotBlank(defaultPackage) ? Collections.singletonList(defaultPackage) : new ArrayList<>();
        setDefaultPackages(pkg);
    }
    public void setDefaultPackages(List<String> newPackages) {
        defaultPackages.clear();
        defaultPackages.addAll(newPackages);
        if (allowPackageChoice) {
            if (!isRememberPackageSelection) {
                selectedPackages.clear();
                selectedPackages.addAll(newPackages);
                newPackageChosen.setText(String.join(",", selectedPackages));
            }
        } else {
            newPackageText.setText(String.join(",", defaultPackages));
        }
    }
    public List<String> getSelectedPackages() {
        if (allowPackageChoice) {
            return new ArrayList<>(selectedPackages);
        } else {
            return new ArrayList<>(defaultPackages);
        }
    }
    public boolean hasSelectedPackage() {
        return getSelectedPackages().size() > 0;
    }

    public void fillDriveList(List<TbDeviceInfoHolder> roots, int selection) {
        driveList.removeAllItems();
        roots.forEach(r -> driveList.addItem(r));
        if (selection >= 0) {
            driveList.setSelectedIndex(selection);
        }
    }
    public TbDeviceInfo getSelectedDevice() {
        TbDeviceInfoHolder deviceInfoHolder = (TbDeviceInfoHolder) driveList.getSelectedItem();
        if (deviceInfoHolder == null) return null;
        return deviceInfoHolder.getDeviceInfo();
    }

    public ProgressDisplayManager getProgressDisplayManager() {
        return progressDisplayManager;
    }

    public void fillPrevDeploymentInfo(DeploymentInfo oldDeploymentInfo, TbDeviceInfo oldDeviceInfo) {
        if (oldDeploymentInfo != null) {
            oldSrnText.setText(oldDeploymentInfo.getSerialNumber());
            oldPackageText.setText(String.join(",", oldDeploymentInfo.getPackageNames()));
            oldDeploymentText.setText(oldDeploymentInfo.getDeploymentName());
            lastUpdatedText.setText(oldDeploymentInfo.getUpdateTimestamp());

            forceTbId.setVisible(tbIdStrategy.allowsManual());
            forceTbId.setSelected(false);

            Recipient recipient = programSpec.getRecipients().getRecipient(oldDeploymentInfo.getRecipientid());
            if (recipient != null) {
                oldRecipientChooser.setSelectedRecipient(oldDeploymentInfo.getRecipientid());
                oldRecipientLayout.show(oldRecipientContainer, "internal");
            } else {
                String recipientInfo = "This Talking Book was in a different program:\n" +
                        oldDeviceInfo.getDeploymentPropertiesString();
                oldRecipientText.setText(recipientInfo);
                oldRecipientLayout.show(oldRecipientContainer, "external");
            }

            testDeployment.setSelected(false);

        } else {
            forceTbId.setVisible(false);
            forceTbId.setSelected(false);

            oldSrnText.setText("");
            oldPackageText.setText("");
            oldDeploymentText.setText("");
            forceFirmware.setSelected(false);
            oldRecipientChooser.setSelectedRecipient(null);
            oldRecipientText.setText("");
            oldRecipientLayout.show(oldRecipientContainer, "internal");
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

    private Consumer<TbDeviceInfo> deviceSelectedListener;
    public void setDeviceSelectedListener(Consumer<TbDeviceInfo> deviceSelectedListener) {
        this.deviceSelectedListener = deviceSelectedListener;
    }

    private Consumer<TbDeviceInfo.DEVICE_VERSION> deviceVersionSelectedListener;
    public void setDeviceVersionSelectedListener(Consumer<TbDeviceInfo.DEVICE_VERSION> deviceVersionSelectedListener) {
        this.deviceVersionSelectedListener = deviceVersionSelectedListener;
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
     * Reset the manual device version selection, ie, set it back to "auto".
     */
    void resetDeviceVersion() {
        if (deviceVersionChanging) {
            System.out.println("Reset device version while device version changing.");
            SwingUtilities.invokeLater(()-> deviceVersionBox.setSelectedIndex(0));
        } else {
            deviceVersionBox.setSelectedIndex(0);
        }
    }

    TbDeviceInfo.DEVICE_VERSION getSelectedDeviceVersion() {
        TbDeviceInfo.DEVICE_VERSION version = TbDeviceInfo.DEVICE_VERSION.NONE;
        if (!tbLoaderConfig.hasTbV2Devices())
            version = TbDeviceInfo.DEVICE_VERSION.TBv1;
        else if (deviceVersionBox.getSelectedIndex() == 1)
            version = TbDeviceInfo.DEVICE_VERSION.TBv1;
        else if (deviceVersionBox.getSelectedIndex() == 2)
            version = TbDeviceInfo.DEVICE_VERSION.TBv2;
        return version;
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
        //noinspection UnusedAssignment
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
                recipientChooser, oldRecipientContainer, newPackageContainer, oldPackageText, datePicker,
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
        boolean isBorrowed = authInstance.getTbSrnHelper().isBorrowedId();
        String deviceIdHex = authInstance.getTbSrnHelper().getTbSrnAllocationInfo().getTbloaderidHex();
        String greetingString = String.format("<html><nobr>Hello <b>%s</b>! <i><span style='font-size:0.85em;color:gray'>(%sTB-Loader ID: %s, version: %s)</span></i></nobr></html>",
                authInstance.getUserSelfName(),
            isBorrowed?"Using ":"", deviceIdHex, ACM_VERSION);
        JLabel greeting = new JLabel(greetingString);
        greetingBox.add(greeting);
        greetingBox.add(Box.createHorizontalStrut(10));

        if (ACMConfiguration.getInstance().isDevo()) {
            JButton xpr = new JButton("Experimental");
            xpr.addActionListener(Experimental::go);
            greetingBox.add(xpr);
            greetingBox.add(Box.createHorizontalStrut(10));
        }

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

        // Add the control to manually select device version, visible if the program has v2 devices.
        // (Always add it, in case user turns on option for TBv2 devices.)
        deviceVersionBox = new JComboBox<>(new String[]{"Auto", "TBv1", "TBv2"});
        deviceVersionBox.addItemListener(this::onDeviceVersionSelected);
        deviceVersionBox.setRenderer(new ComboBoxRenderer());
        deviceVersionBox.setVisible(tbLoaderConfig.hasTbV2Devices());
        deviceBox.add(deviceVersionBox);
        deviceBox.add(Box.createHorizontalStrut(10));


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
        recipientChooser.addActionListener(this::onRecipientSelected);

        oldRecipientChooser = new JRecipientChooser();
        oldRecipientChooser.populate(programSpec);
        oldRecipientChooser.setEnabled(false);
        oldRecipientChooser.setHighlightWhenNoSelection(false);

        oldRecipientText = new JTextArea();
        oldRecipientText.setEditable(false);
        oldRecipientText.setLineWrap(true);
        oldRecipientText.setWrapStyleWord(true);
        oldRecipientText.setBorder(new LineBorder(Color.DARK_GRAY, 1));

        oldRecipientLayout = new CardLayout();
        oldRecipientContainer = new JPanel(oldRecipientLayout);
        oldRecipientContainer.add(oldRecipientChooser, "internal");
        oldRecipientContainer.add(oldRecipientText, "external");
        oldRecipientLayout.show(oldRecipientContainer, "internal");

        // Recipient Chooser.
        contentPanel.add(communityLabel, gbc.withGridx(0));
        contentPanel.add(recipientChooser, gbc);
        contentPanel.add(oldRecipientContainer, gbc);
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
        Box chooserBox = Box.createHorizontalBox();
        newPackageChosen = new JTextField();
        newPackageChosen.setEditable(false);
        JButton openChooser = new JButton("Choose");
        openChooser.addActionListener(this::choosePackage);
        chooserBox.add(newPackageChosen);
        chooserBox.add(Box.createHorizontalStrut(5));
        chooserBox.add(openChooser);

        // For when it is false
        newPackageText = new JTextField();
        newPackageText.setEditable(false);

        newPackageLayout = new CardLayout();
        newPackageContainer = new JPanel(newPackageLayout);
        newPackageContainer.add(chooserBox, "CHOICE");
        newPackageContainer.add(newPackageText, "NOCHOICE");
        showNewPackage();

        oldPackageText = new JTextField();
        oldPackageText.setEditable(false);

        contentPanel.add(contentPackageLabel, gbc.withGridx(0));
        contentPanel.add(newPackageContainer, gbc);
        contentPanel.add(oldPackageText, gbc);
    }

    /**
     * Handles the "Choose" package button.
     * @param actionEvent triggering event. We get the button, from which we get the location, from this.
     */
    private void choosePackage(ActionEvent actionEvent) {
        Component c = actionEvent.getSource() instanceof Component ? (Component)actionEvent.getSource() : null;
        Point p = c != null ? getApplicationRelativeLocation(c) : new Point(20,20);
        SelectPackagesDialog dialog = new SelectPackagesDialog(null,
            Arrays.asList(packagesInDeployment),
            packageNameMap,
            defaultPackages,
            selectedPackages,
            isRememberPackageSelection,
            isTbV2()?MAX_PACKAGES_TBV2:MAX_PACKAGES_TBV1);
        UIUtils.showDialog(dialog, p.x, p.y);

        if (dialog.isOk()) {
            selectedPackages = dialog.getSelectedPackages();
            isRememberPackageSelection = dialog.isRememberSelection();
            newPackageChosen.setText(String.join(",", selectedPackages));
        }
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

        updateTb2FirmwareBox = Box.createHorizontalBox();
        if (builder.updateTb2FirmwareListener != null) {
            updateTb2FirmwareButton = new JButton("Update TB Firmware");
            updateTb2FirmwareButton.addActionListener(e->builder.updateTb2FirmwareListener.get());
            updateTb2FirmwareBox.setVisible(tbLoaderConfig.hasTbV2Devices());
            updateTb2FirmwareBox.add(updateTb2FirmwareButton);
            updateTb2FirmwareBox.add(Box.createHorizontalStrut(10));
        }

        actionChooser = new JComboBox<>(actionList);
        actionChooser.addActionListener(e->setEnabledStates());
        goButton = new JButton("Go!");
        defaultButtonBackgroundColor = goButton.getBackground();
        defaultButtonForegroundColor = goButton.getForeground();
        goButton.setEnabled(false);
        goButton.setForeground(Color.GRAY);
        goButton.setOpaque(true);
        goButton.addActionListener(this::onGoButton);
        Box actionBox = Box.createHorizontalBox();
        actionBox.add(Box.createHorizontalGlue());
        actionBox.add(updateTb2FirmwareBox);
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

    boolean deviceVersionChanging = false;
    private void onDeviceVersionSelected(ItemEvent itemEvent) {
        // Is this a new value? Are we already working on a change?
        if (itemEvent.getStateChange() != ItemEvent.SELECTED || deviceVersionChanging || deviceSelectionChanging) return;
        deviceVersionChanging = true;
        try {
            if (deviceSelectedListener != null) {
                deviceSelectedListener.accept(getSelectedDevice());
            }
            setEnabledStates();
        } finally {
            deviceVersionChanging = false;
        }
    }

    private boolean deviceSelectionChanging = false;
    private void onTbDeviceSelected(ItemEvent e) {
        // If not a new value, or if in the middle of changing devices, do nothing.
        if (e.getStateChange() != ItemEvent.SELECTED || deviceSelectionChanging) return;
        try {
            deviceSelectionChanging = true;
            // Reset the device version to "auto".
            if (deviceVersionBox.getSelectedIndex() != 0) {
                deviceVersionBox.setSelectedIndex(0);
            }
            if (deviceSelectedListener != null) {
                deviceSelectedListener.accept(getSelectedDevice());
            }
            // May need to update the device version display, but no user activity or selection has happened to cause
            // it to update. So, repaint it manually.
            deviceVersionBox.repaint();
            setEnabledStates();
        } finally {
            deviceSelectionChanging = false;
        }
    }

    private void onRecipientSelected(ActionEvent actionEvent) {
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

    /**
     * The "Go" button was clicked. Perform the update, collect stats, whatever.
     * @param actionEvent is ignored.
     */
    private synchronized void onGoButton(ActionEvent actionEvent) {
        if (!isEnabled()) {
            setEnabledStates();
            return;
        }
        TBLoader.Operation operation;
        //noinspection ConstantConditions
        boolean isUpdate = actionChooser.getSelectedItem()
            .toString()
            .equalsIgnoreCase(UPDATE_TB);
        operation = isUpdate ? TBLoader.Operation.Update : TBLoader.Operation.CollectStats;
        goListener.accept(operation);
    }

    private boolean isTbV2() {
        if (!tbLoaderConfig.hasTbV2Devices()) return false;
        return getDeviceVersion() == TbDeviceInfo.DEVICE_VERSION.TBv2;
    }
    private boolean isTbV1() {
        return getDeviceVersion() == TbDeviceInfo.DEVICE_VERSION.TBv1;
    }

    /**
     * Determines whether a TBv2 fimrware update is required, by comparing the device-reported firmware version
     * with the deployment-required version.
     * @return true if a firmware update is required.
     */
    private boolean isV2FirmwareUpdateRequired() {
        if (isTbV2()) {
            String oldVersion = oldFirmwareVersionText.getText();
            String newVersion = newFirmwareVersionText.getText();
            return !oldVersion.equals(newVersion);
        }
        return false;
    }

    /**
     * Checkes whether a TBv2 firmware update is required, and sets the "update firmware" button text
     * and colors appropriately.
     */
    private void checkV2FirmwareUpdateRequired() {
        if (isTbV2()) {
            boolean updateRequired = isV2FirmwareUpdateRequired();
            updateTb2FirmwareButton.setBackground(updateRequired ? Color.GREEN : defaultButtonBackgroundColor);
            updateTb2FirmwareButton.setForeground(updateRequired ? Color.BLACK : defaultButtonForegroundColor);
            if (updateRequired) {
                updateTb2FirmwareButton.setText("Perform Required Firmware Update");
                updateTb2FirmwareButton.setToolTipText("The TBv2 firmware must be updated to continue.");
            } else {
                updateTb2FirmwareButton.setText("Update TB Firmware");
                updateTb2FirmwareButton.setToolTipText("A TBv2 firmware update is not required at this time.");
            }
            updateTb2FirmwareButton.setEnabled(tbLoaderConfig.hasDfuSupport());
            if (!tbLoaderConfig.hasDfuSupport()) {
                updateTb2FirmwareButton.setToolTipText("<html>TBv2 support has not been installed.<br/> See https://downloads.amplio.org/software/index.html.");
            }
        }
    }

    private void setEnabledStates() {
        boolean isUpdate = actionChooser.getSelectedItem() != null && actionChooser.getSelectedItem()
            .toString()
            .equalsIgnoreCase(UPDATE_TB);
        // Must have device (may be a pseuo device). Update must not be in progress.
        boolean enabled = (isDriveConnected() || tbLoaderConfig.isPseudoTb()) && isEnabled();
        // Must have set location. TODO: why?
        enabled = enabled && (currentLocationChooser.getSelectedIndex() != 0);
        // To update...
        if (isUpdate) {
            // Must have community.
            enabled = enabled && (getSelectedRecipient() != null);
            // Must not require updating (TBv2) firmware.
            enabled = enabled && !(isV2FirmwareUpdateRequired() && tbLoaderConfig.isStrictTbV2FIrmware());
        }
        enabled = enabled && (getDeviceVersion() == TbDeviceInfo.DEVICE_VERSION.TBv1 ||
                getDeviceVersion() == TbDeviceInfo.DEVICE_VERSION.TBv2);
        goButton.setEnabled(enabled);
        goButton.setBackground(enabled ? Color.GREEN : defaultButtonBackgroundColor);
        goButton.setForeground(enabled ? Color.BLACK : Color.GRAY);

        recipientChooser.setHighlightWhenNoSelection(isUpdate);
        deviceVersionBox.setVisible(tbLoaderConfig.hasTbV2Devices());
        boolean offerFirmware = (isUpdate || tbLoaderConfig.offerTbV2FirmwareWithStats()) && isTbV2() && isEnabled();
        updateTb2FirmwareBox.setVisible(offerFirmware);
        forceFirmware.setVisible(isUpdate && isTbV1());
    }

    /**
     * Is there a device connected, and is it believed to be a Talking Book?
     * @return true if the device is (or is to be treated as) a Talking Book.
     */
    private synchronized boolean isDriveConnected() {
        boolean connected = false;
        TbFile drive;

        if (driveList.getItemCount() > 0) {
            TbDeviceInfo tbDeviceInfo = getSelectedDevice();
            drive = tbDeviceInfo==null?null:tbDeviceInfo.getRootFile();
            if (drive != null) connected = true;
        }
        return connected;
    }

    /**
     * Gets the device version as chosen by the user. Usually this will be automatically determined from the physical
     * device, but in the event the user needs to, they can over-ride that with a manual selection.
     * @return the device version to use, TBv1, TBv2, or NONE if not automatically detectable and not overridden.
     */
    public TbDeviceInfo.DEVICE_VERSION getDeviceVersion() {
        TbDeviceInfo deviceInfo = getSelectedDevice();
        if (deviceInfo == null)
            return TbDeviceInfo.DEVICE_VERSION.NONE;
        if (deviceVersionBox.getSelectedItem()!=null && deviceVersionBox.getSelectedItem().toString().equals("TBv1")) {
            return TbDeviceInfo.DEVICE_VERSION.TBv1;
        } else if (deviceVersionBox.getSelectedItem()!=null && deviceVersionBox.getSelectedItem().toString().equals("TBv2")) {
            return TbDeviceInfo.DEVICE_VERSION.TBv2;
        } else {
            // Auto-detect device version
            return getPhysicalDeviceVersion();
        }
    }

    /**
     * Gets what sort of device we think it is, based on examining the contents of the file system.
     * @return The device version.
     */
    private TbDeviceInfo.DEVICE_VERSION getPhysicalDeviceVersion() {
        TbDeviceInfo deviceInfo = getSelectedDevice();
        if (deviceInfo == null) {
            return TbDeviceInfo.DEVICE_VERSION.NONE;
        } else if (tbLoaderConfig.hasTbV2Devices()) {
            return deviceInfo.getDeviceVersion();
        } else {
            return TbDeviceInfo.DEVICE_VERSION.TBv1;
        }
    }


    /**
     * Class to render the labels in the device version combo box. There are three entries in the dropdown: auto,
     * TBv1, and TBv2. Choosing TBv1 or TBv2 treats any attached device as indicated. Choosing auto picks a device
     * version based on files found on the device.
     *
     * The class will display the labels correctly in the dropdown, and will decorate the "Auto" with "v1" or "v2"
     * based on the device found, in the display.
     */
    class ComboBoxRenderer extends JLabel implements ListCellRenderer<String> {
        public ComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
        }

        public Component getListCellRendererComponent(
            JList<? extends String> list,
            String value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

            int selectedIndex = deviceVersionBox.getSelectedIndex();

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            
            switch (index) {
                // Display the proper labels in the dropdown.
                case 0: setText("Auto"); break;
                case 1: setText("TBv1"); break;
                case 2: setText("TBv2"); break;
                default:
                    // And display the net setting in the main control.
                    switch (selectedIndex) {
                        case 1: setText("TBv1"); break;
                        case 2: setText("TBv2"); break;
                        default:
                        if (isTbV1()) {
                            setText("V1 (auto)");
                        } else if (isTbV2()) {
                            setText("V2 (auto)");
                        } else {
                            setText("Auto");
                        }
                    }
            }
            return this;
        }
    }

}

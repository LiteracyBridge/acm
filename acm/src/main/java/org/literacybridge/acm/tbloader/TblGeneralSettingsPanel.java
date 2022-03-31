package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.settings.AbstractSettingsBase;
import org.literacybridge.acm.gui.settings.AbstractSettingsDialog;
import org.literacybridge.core.OSChecker;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collection;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;

public class TblGeneralSettingsPanel extends AbstractSettingsBase {
    private final GBC protoGbc;
    private final JPanel settingsPanel;
    public JCheckBox isStrictTbV2FirmwareCB;
    // Note min is GB, max is GiB. Because drive manufacturers will "round up" their sizes.
    private JCheckBox min1GB;
    private JCheckBox max16GiB;
    private JCheckBox asUsbDrive;
    private JCheckBox allowPackageChoice;
    private JComboBox<String> srnStrategyCombo;
    private JComboBox<String> testStrategyCombo;
    private JCheckBox hasTbV2DevicesCB;
    private JCheckBox offerTbV2FirmwareWithStats;
    private JCheckBox isSuppressDosToolsCB;

    @Override
    public String getTitle() {
        return "General Settings";
    }

    TblGeneralSettingsPanel(AbstractSettingsDialog.SettingsHelper helper) {
        super(helper);

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 10, 10, 10));

        // An intermediate panel, with a nice border.
        JPanel borderPanel = new JPanel(new BorderLayout());
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setBorder(new RoundedLineBorder(new Color(112, 154, 208), 1, 6));

        // The inner panel, to hold the grid. Also has an empty border, to give some blank space.
        settingsPanel = new JPanel(new GridBagLayout());
        borderPanel.add(settingsPanel, BorderLayout.CENTER);
        settingsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        protoGbc = new GBC().setInsets(new Insets(0, 3, 10, 2))
                            .setAnchor(LINE_START)
                            .setFill(HORIZONTAL);
        int y = 0;

        addMinimumUsbCapacitySetting(y++);
        addMaximumUsbCapacitySetting(y++);
        addRequiredLabelSetting(y++);
        addAllowPackageChoiceSetting(y++);
        addHasTbV2DevicesSetting(y++);
        addStrictTbV2Firmware(y++);
        addOfferTbV2FirmwareWithStats(y++);
        addSuppressDosTools(y++);
        addSrnStrategy(y++);
        addTestStrategy(y++);

        // Consume any blank space.
        settingsPanel.add(new JLabel(), protoGbc.withGridy(y).setWeighty(1));

        FsRootMonitor.FilterParams filterParams = TBLoader.getApplication()
                                                          .getFsRootMonitor()
                                                          .getFilterParams();
        min1GB.setSelected(Math.abs(filterParams.minSize-1L*1000*1000*1000) < 10L*1000*1000);
        max16GiB.setSelected(Math.abs(filterParams.maxSize-16L*1024*1024*1024) < 10L*1024*1024);
        asUsbDrive.setSelected(filterParams.allowedLabels.contains("USB Drive"));

        helper.setValid(true);
    }

    private void addMinimumUsbCapacitySetting(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("TBs are not smaller than"), gbc.withGridx(0));
        min1GB = new JCheckBox("1 GB", true);
        settingsPanel.add(min1GB, gbc.withWeightx(1));
    }

    private void addMaximumUsbCapacitySetting(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("TBs are not larger than"), gbc.withGridx(0));
        max16GiB = new JCheckBox("16 GB", true);
        settingsPanel.add(max16GiB, gbc);
    }

    static String joiner(Collection<String> elements) {
        StringBuilder joined = new StringBuilder();
        int n = 0;
        for (CharSequence cs: elements) {
            if (++n > 1) {
                joined.append(n==elements.size() ? ", or " : ", ");
            }
            joined.append('\'').append(cs).append('\'');
        }
        return joined.toString();
    }

    private void addRequiredLabelSetting(int y) {
        FsRootMonitor.FilterParams filterParams = TBLoader.getApplication().getFsFilterParams();
        String labels = joiner(filterParams.allowedLabels);
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("TBs are labelled as"), gbc.withGridx(0));
        asUsbDrive = new JCheckBox(labels, OSChecker.WINDOWS);
        settingsPanel.add(asUsbDrive, gbc);
    }

    private void addAllowPackageChoiceSetting(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("Allow package override"), gbc.withGridx(0));
        allowPackageChoice = new JCheckBox("Choose alternative package for recipients.");
        settingsPanel.add(allowPackageChoice, gbc);
        allowPackageChoice.setSelected(TBLoader.getApplication().allowsPackageChoice());
    }

    private void addSrnStrategy(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("Talking Book IDs"), gbc.withGridx(0));
        String[] srnStrategies = Arrays.stream(TBLoader.TB_ID_STRATEGY.values())
                .map(v->v.description)
                .map(LabelProvider::getLabel)
                .toArray(String[]::new);
        srnStrategyCombo = new JComboBox<>(srnStrategies);
        srnStrategyCombo.setSelectedIndex(TBLoader.getApplication().getTbIdStrategy().ordinal());
        settingsPanel.add(srnStrategyCombo, gbc);
    }

    private void addTestStrategy(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("'Test deployment' strategy"), gbc.withGridx(0));
        String[] srnStrategies = Arrays.stream(TBLoader.TEST_DEPLOYMENT_STRATEGY.values())
                .map(v->v.description)
                .map(LabelProvider::getLabel)
                .toArray(String[]::new);
        testStrategyCombo = new JComboBox<>(srnStrategies);
        testStrategyCombo.setSelectedIndex(TBLoader.getApplication().getTestStrategy().ordinal());
        settingsPanel.add(testStrategyCombo, gbc);
    }

    private void addHasTbV2DevicesSetting(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("Version-2 Talking Books"), gbc.withGridx(0));
        hasTbV2DevicesCB = new JCheckBox("There are Version-2 Talking Books in the program.");
        settingsPanel.add(hasTbV2DevicesCB, gbc);
        hasTbV2DevicesCB.setSelected(TBLoader.getApplication().getHasTbV2Devices());
    }


    private void addStrictTbV2Firmware(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("Strict TBv2 Firmware"), gbc.withGridx(0));
        isStrictTbV2FirmwareCB = new JCheckBox("Require TBv2 Firmware updates before content.");
        isStrictTbV2FirmwareCB.setToolTipText("When a TBv2 device needs a firmware update, require the update before loading content.");
        settingsPanel.add(isStrictTbV2FirmwareCB, gbc);
        isStrictTbV2FirmwareCB.setSelected(TBLoader.getApplication().isStrictTbV2Firmware());
    }

    private void addOfferTbV2FirmwareWithStats(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("Update TBv2 Firmware with Stats"), gbc.withGridx(0));
        offerTbV2FirmwareWithStats = new JCheckBox("Offer TBv2 Firmware updates when collecting stats.");
        offerTbV2FirmwareWithStats.setToolTipText("When a TBv2 device needs a firmware update, offer the update when collecting stats.");
        settingsPanel.add(offerTbV2FirmwareWithStats, gbc);
        offerTbV2FirmwareWithStats.setSelected(TBLoader.getApplication().offerTbV2FirmwareWithStats());
    }

    private void addSuppressDosTools(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("No MSDOS tools."), gbc.withGridx(0));
        isSuppressDosToolsCB = new JCheckBox("Don't run external MSDOS commands, like 'chkdsk'.");
        isSuppressDosToolsCB.setToolTipText("Running external commands can be problematic on non-English systems.");
        settingsPanel.add(isSuppressDosToolsCB, gbc);
        isSuppressDosToolsCB.setSelected(TBLoader.getApplication().isSuppressDosTools());
    }


    @Override
    public void onCancel() {
        // Nothing to do.
    }

    /**
     * Save any settings that have changed.
     */
    @Override
    public void onOk() {
        TBLoader tbLoaderApp = TBLoader.getApplication();
        FsRootMonitor.FilterParams filterParams = new FsRootMonitor.FilterParams();

        if (min1GB.isSelected()) filterParams.minimum(1);
        if (max16GiB.isSelected()) filterParams.maximum(16);
        if (asUsbDrive.isSelected()) filterParams.allowing("USB Drive");

        tbLoaderApp.setFsFilterParams(filterParams);
        tbLoaderApp.setTbIdStrategy(srnStrategyCombo.getSelectedIndex());
        tbLoaderApp.setAllowPackageChoice(allowPackageChoice.isSelected());
        tbLoaderApp.setTestStrategy(testStrategyCombo.getSelectedIndex());
        tbLoaderApp.setHasTbV2Devices(hasTbV2DevicesCB.isSelected());
        tbLoaderApp.setStrictTbV2Firmware(isStrictTbV2FirmwareCB.isSelected());
        tbLoaderApp.offerTbV2FirmwareWithStats(offerTbV2FirmwareWithStats.isSelected());
        tbLoaderApp.setSuppressDosTools(isSuppressDosToolsCB.isSelected());
    }

    /**
     * Called to query if the settings are all valid. We only validate the fuzzy threshold.
     *
     * @return true if the current value is valid, false if not.
     */
    @Override
    public boolean settingsValid() {
        return true;
    }

}

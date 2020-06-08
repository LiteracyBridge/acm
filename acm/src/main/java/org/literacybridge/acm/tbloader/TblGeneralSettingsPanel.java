package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.settings.AbstractSettingsBase;
import org.literacybridge.acm.gui.settings.AcmSettingsDialog;
import org.literacybridge.core.OSChecker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Insets;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;

public class TblGeneralSettingsPanel extends AbstractSettingsBase {
    private final GBC protoGbc;
    private final JPanel settingsPanel;
    // Note min is GB, max is GiB. Because drive manufacturers will "round up" their sizes.
    private JCheckBox min2GB;
    private JCheckBox max16GiB;
    private JCheckBox asUsbDrive;

    @Override
    public String getTitle() {
        return "General Settings";
    }

    TblGeneralSettingsPanel(AcmSettingsDialog.SettingsHelper helper) {
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

        layoutMinimumSize(y++);
        layoutMaximumSize(y++);
        layoutRequiredLabel(y++);

        // Consume any blank space.
        settingsPanel.add(new JLabel(), protoGbc.withGridy(y).setWeighty(1));

        FsRootMonitor.FilterParams filterParams = TBLoader.getApplication()
                                                          .getFsRootMonitor()
                                                          .getFilterParams();
        min2GB.setSelected(Math.abs(filterParams.minSize-2L*1000*1000*1000) < 10L*1000*1000);
        max16GiB.setSelected(Math.abs(filterParams.maxSize-16L*1024*1024*1024) < 10L*1024*1024);
        asUsbDrive.setSelected(filterParams.allowedLabels.contains("USB Drive"));

        helper.setValid(true);
    }

    private void layoutMinimumSize(int y) {
        GBC gbc = new GBC(protoGbc).setGridy(y);
        settingsPanel.add(new JLabel("TBs are not smaller than"), gbc.withGridx(0));
        min2GB = new JCheckBox("2 GB", true);
        settingsPanel.add(min2GB, gbc.withWeightx(1));
    }

    private void layoutMaximumSize(int y) {
        GBC gbc = new GBC(protoGbc).setGridy(y);
        settingsPanel.add(new JLabel("TBs are not larger than"), gbc.withGridx(0));
        max16GiB = new JCheckBox("16 GB", true);
        settingsPanel.add(max16GiB, gbc);
    }

    private void layoutRequiredLabel(int y) {
        GBC gbc = new GBC(protoGbc).setGridy(y);
        settingsPanel.add(new JLabel("TBs are labelled as"), gbc.withGridx(0));
        asUsbDrive = new JCheckBox("USB Drive", OSChecker.WINDOWS);
        settingsPanel.add(asUsbDrive, gbc);
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
        FsRootMonitor.FilterParams filterParams = new FsRootMonitor.FilterParams();

        if (min2GB.isSelected()) filterParams.minimum(2);
        if (max16GiB.isSelected()) filterParams.maximum(16);
        if (asUsbDrive.isSelected()) filterParams.allowing("USB Drive");

        TBLoader.getApplication().getFsRootMonitor().setFilterParams(filterParams);
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

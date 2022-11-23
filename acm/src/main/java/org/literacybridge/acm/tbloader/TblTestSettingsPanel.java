package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.dialogs.PopUp;
import org.literacybridge.acm.gui.settings.AbstractSettingsBase;
import org.literacybridge.acm.gui.settings.AbstractSettingsDialog;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.LINE_START;

public class TblTestSettingsPanel extends AbstractSettingsBase {
    private final GBC protoGbc;
    private final JPanel settingsPanel;

    private JTextField pseudoTbDir;
    private JCheckBox isPseudoTalkingBookCB;
    private JCheckBox doNotUploadStatsCB;
    
    @Override
    public String getTitle() {
        return "General Settings";
    }

    TblTestSettingsPanel(AbstractSettingsDialog.SettingsHelper helper) {
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

        addPseudoTbDir(y++);
        addPseudoTB(y++);
        addDoNotUpload(y++);

        // Consume any blank space.
        settingsPanel.add(new JLabel(), protoGbc.withGridy(y).setWeighty(1));
        
        helper.setValid(true);
    }

    private void addPseudoTbDir(int y) {
        GBC gbc = protoGbc.withGridy(y);

        settingsPanel.add(new JLabel("Pseudo TB directory:"), gbc.withGridx(0));
        pseudoTbDir = new JTextField("pseudoTb");
        pseudoTbDir.setToolTipText(
            "A directory that will receive pseudo-talking book images.");
        // We don't want/need tab characters, so use them for navigation. As the user would expect.
        pseudoTbDir.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.getModifiers() > 0) {
                        pseudoTbDir.transferFocusBackward();
                    } else {
                        pseudoTbDir.transferFocus();
                    }
                    e.consume();
                }
            }
        });

        Insets insets = gbc.insets;
        // Add a little padding on the bottom, so the pseudoTbDir box doesn't sit right on top of the fuzzy edit box.
        settingsPanel.add(pseudoTbDir, gbc.withWeightx(1).withInsets(new Insets(insets.top, insets.left, 8, insets.right)));
    }


    private void addPseudoTB(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("Pseudo TB:"), gbc.withGridx(0));
        isPseudoTalkingBookCB = new JCheckBox("Write data to a local file?");
        isPseudoTalkingBookCB.setToolTipText("Simulate TB file system.");
        settingsPanel.add(isPseudoTalkingBookCB, gbc);
        isPseudoTalkingBookCB.setSelected(true);
    }

    private void addDoNotUpload(int y) {
        GBC gbc = protoGbc.withGridy(y);
        settingsPanel.add(new JLabel("Do Not Upload:"), gbc.withGridx(0));
        doNotUploadStatsCB = new JCheckBox("Don't upload stats.");
        doNotUploadStatsCB.setToolTipText("Delete stats files, to avoid disturbing production data.");
        settingsPanel.add(doNotUploadStatsCB, gbc);
        doNotUploadStatsCB.setSelected(TBLoader.getApplication().tbLoaderConfig.isDoNotUpload());
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
        tbLoaderApp.setPseudoTb( isPseudoTalkingBookCB.isSelected() ? pseudoTbDir.getText() : null );
        if (isPseudoTalkingBookCB.isSelected()) {
            TBLoader.getApplication().setTestMode(true);
            // Suppress the "cannot find the statistics"dialog for the pseudo device.
            PopUp.setOptOutValue(TBLoader.CANNOT_FIND_THE_STATISTICS, 0);
        }
        TBLoader.getApplication().setDoNotUpload(doNotUploadStatsCB.isSelected());
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

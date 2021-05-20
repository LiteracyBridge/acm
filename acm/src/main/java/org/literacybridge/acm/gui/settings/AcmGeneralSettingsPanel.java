package org.literacybridge.acm.gui.settings;

import com.formdev.flatlaf.FlatLaf;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextArea;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AcmGeneralSettingsPanel extends AbstractSettingsBase {
    private final PlaceholderTextArea email;
    private final JTextField fuzzyThreshold;
    private final String emailAddresses;
    private final int threshold;
    private final boolean warnForMissingGreetings;
    private final JLabel fuzzyThresholdError;
    private final JCheckBox greetingWarnings;

    @Override
    public String getTitle() {
        return "General Settings";
    }

    AcmGeneralSettingsPanel(AbstractSettingsDialog.SettingsHelper helper) {
        super(helper);

        // Get values from the global configuration.
        threshold = ACMConfiguration.getInstance().getCurrentDB().getFuzzyThreshold();
        emailAddresses = String.join(", ",
                ACMConfiguration.getInstance().getCurrentDB().getNotifyList());
        warnForMissingGreetings = ACMConfiguration.getInstance().getCurrentDB().getWarnForMissingGreetings();

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 10, 10, 10));

        add(new JLabel(LabelProvider.getLabel("General settings, apply to the entire program.")), BorderLayout.NORTH);

        // An intermediate panel, with a nice border.
        JPanel borderPanel = new JPanel(new BorderLayout());
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setBorder(new RoundedLineBorder(new Color(112, 154, 208), 1, 6));

        // The inner panel, to hold the grid. Also has an empty border, to give some blank space.
        JPanel gridPanel = new JPanel(new GridBagLayout());
        borderPanel.add(gridPanel, BorderLayout.CENTER);
        gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Constraints for the left column.
        GBC gbcLeft = new GBC()
                .setAnchor(GridBagConstraints.FIRST_LINE_END)
                .setFill(GridBagConstraints.NONE)
                .setIpady(10)
                .setGridx(0);
        // Constraints for the right column.
        GBC gbcRight = new GBC()
                .setAnchor(GridBagConstraints.LINE_START)
                .setFill(GridBagConstraints.HORIZONTAL)
                .setIpady(10)
                .setWeightx(1.0)
                .setGridx(1);

        // First setting: notification email addresses.
        gridPanel.add(new JLabel("Email notifications:"), gbcLeft);
        email = new PlaceholderTextArea("", 3, 0);
        email.setPlaceholder("me@example.com, you@example.com");
        email.setToolTipText(
            "A list of email addresses, separated by commas or spaces,\nto receive notifications about content import and deployments.");
        // We don't want/need tab characters, so use them for navigation. As the user would expect.
        email.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.getModifiers() > 0) {
                        email.transferFocusBackward();
                    } else {
                        email.transferFocus();
                    }
                    e.consume();
                }
            }
        });

        email.setLineWrap(true);
        email.setWrapStyleWord(true);
        Insets insets = gbcRight.insets;
        // Add a little padding on the bottom, so the email box doesn't sit right on top of the fuzzy edit box.
        gridPanel.add(email, gbcRight.withInsets(new Insets(insets.top, insets.left, 8, insets.right)));

        // Setting: fuzzy matching threshold.
        gridPanel.add(new JLabel("Fuzzy threshold:"), gbcLeft);
        Box vbox = Box.createVerticalBox();
        fuzzyThreshold = new JTextField("80");
        fuzzyThreshold.setInputVerifier(thresholdVerifier);
        fuzzyThreshold.getDocument().addDocumentListener(thresholdDocumentListener);
        fuzzyThreshold.setToolTipText(
            "A number between 60 and 100, to control the strictness of \"fuzzy matching\". A value of 100 means \"completely strict\".");
        vbox.add(fuzzyThreshold);
        fuzzyThresholdError = new JLabel("Threshold must be an integer between 60 and 100.");
        fuzzyThresholdError.setFont(new Font("Sans Serif", Font.ITALIC, 10));
        fuzzyThresholdError.setForeground(Color.RED);
        fuzzyThresholdError.setVisible(false);
        vbox.add(fuzzyThresholdError);
        gridPanel.add(vbox, gbcRight.setFill(GridBagConstraints.NONE));

        // Setting: warn for missing custom greetings.
        gbcLeft.anchor = GridBagConstraints.BASELINE_TRAILING;
        gridPanel.add(new JLabel("Greetings warnings:"), gbcLeft);
        gbcRight.anchor = GridBagConstraints.BASELINE_LEADING;
        greetingWarnings = new JCheckBox("Warn if greetings are missing for deployment", warnForMissingGreetings);
        greetingWarnings.setToolTipText(
                "When creating a Deployment, should you be warned if any Recipients are missing custom greetings?");
        gridPanel.add(greetingWarnings, gbcRight);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //
        // EXPERIMENTAL!! The assistants don't play well with themes.
        //
        // Setting: dark/light theme
        if (helper.isAdvanced() || ACMConfiguration.getInstance().isDevo()) {
            gridPanel.add(new JLabel("Theme:"), gbcLeft);
            String[] themes = {"Light", "Dark", "IntelliJ", "Darcula"};
            JComboBox<String> themeChooser = new JComboBox<>(themes);
            AssistantPage.setComboWidth(themeChooser, themes);
            themeChooser.addActionListener(this::onThemeSelected);
            gridPanel.add(themeChooser, gbcRight.setFill(GridBagConstraints.NONE));
        }
        //
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Consume any blank space.
        gbcLeft.weighty = 1.0;
        gridPanel.add(new JLabel(), gbcLeft);

        email.setText(emailAddresses);
        fuzzyThreshold.setText(String.format("%d", threshold));
    }

    private static final Map<String, String> themeClasses;
    static {
        themeClasses = new HashMap<>();
        themeClasses.put("Light", "com.formdev.flatlaf.FlatLightLaf");
        themeClasses.put("Dark", "com.formdev.flatlaf.FlatDarkLaf");
        themeClasses.put("IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf");
        themeClasses.put("Darcula", "com.formdev.flatlaf.FlatDarculaLaf");
    }
    private void onThemeSelected(ActionEvent actionEvent) {
        Object o = actionEvent.getSource();
        if (o instanceof JComboBox) {
            try {
                //noinspection ConstantConditions
                String theme = ((JComboBox<?>)o).getSelectedItem().toString();
                UIManager.setLookAndFeel(themeClasses.get(theme));
            } catch (Exception e) {
                e.printStackTrace();
            }
            FlatLaf.updateUI();
        }
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
        boolean haveChanges = false;

        if (!email.getText().equalsIgnoreCase(emailAddresses)) {
            Set<String> emailList = Arrays.stream(email.getText().split("[, ]+"))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
            ACMConfiguration.getInstance().getCurrentDB().setNotifyList(emailList);
            haveChanges = true;
        }

        if (getThreshold() != threshold) {
            ACMConfiguration.getInstance().getCurrentDB().setFuzzyThreshold(getThreshold());
            haveChanges = true;
        }

        if (greetingWarnings.isSelected() != warnForMissingGreetings) {
            ACMConfiguration.getInstance().getCurrentDB().setWarnForMissingGreetings(greetingWarnings.isSelected());
            haveChanges = true;
        }

        if (haveChanges) {
            ACMConfiguration.getInstance().getCurrentDB().writeProps();
        }
    }

    /**
     * Called to query if the settings are all valid. We only validate the fuzzy threshold.
     * @return true if the current value is valid, false if not.
     */
    @Override
    public boolean settingsValid() {
        return thresholdVerifier.verify(fuzzyThreshold);
    }

    /**
     * Reads the value of the fuzzy match threshold from its edit control.
     * @return the value, as an integer.
     */
    private int getThreshold() {
        String str = fuzzyThreshold.getText();
        return Integer.parseInt(str);
    }

    /**
     * Listen to changes to the fuzzy match threshold, and validate whenever it changes.
     */
    private final DocumentListener thresholdDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            thresholdVerifier.verify(fuzzyThreshold);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            thresholdVerifier.verify(fuzzyThreshold);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            thresholdVerifier.verify(fuzzyThreshold);
        }
    };

    /**
     * Validator for the fuzzy match threshold field.
     */
    private final InputVerifier thresholdVerifier = new InputVerifier() {
        @Override
        public boolean verify(JComponent input) {
            String newVal = fuzzyThreshold.getText();
            boolean ok = false;
            try {
                int val = Integer.parseInt(newVal);
                ok = val >= 60 && val <= 100;
            } catch (Exception ignored) {

            }
            fuzzyThresholdError.setVisible(!ok);
            helper.setValid(ok);
            return ok;
        }
    };

}

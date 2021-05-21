package org.literacybridge.acm.gui.settings;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.literacybridge.acm.gui.UIConstants.getResource;
import static org.literacybridge.acm.utils.SwingUtils.addEscapeListener;

/**
 * A dialog to implement a set of settings pages. Each page should be a logical grouping of
 * settings.
 */
public abstract class AbstractSettingsDialog extends JDialog {
    /**
     * Class by which the settings panels can communicate with this dialog.
     */
    public class SettingsHelper {
        private final LabelButton button;

        SettingsHelper(LabelButton button) {
            this.button = button;
        }
        public void setValid(boolean valid) {
            okButton.setEnabled(valid);
        }
        public void setEnabled(boolean enabled) {
            this.button.setEnabled(enabled);
        }
        public void setToolTip(String toolTipText) {
            this.button.setToolTipText(toolTipText);
        }

        public boolean isAdvanced() {
            return advanced;
        }
    }

    // Width of the settings topics buttons, on the left side.
    private static final int BUTTONS_WIDTH = 100;
    // Width of the settings panels, on the right side.
    private static final int PANEL_WIDTH = 550;
    private static final int DIALOG_HEIGHT = 700;

    private final CardLayout settingsLayout;
    private final JPanel settingsPanel;
    private final JButton okButton;

    private final Map<String, AbstractSettingsBase> settingsPanels = new LinkedHashMap<>();
    private String currentTag = null;

    private final boolean advanced;

    protected AbstractSettingsDialog(Window owner, String title) {
        this(owner, title, false);
    }
    protected AbstractSettingsDialog(Window owner, String title, boolean advanced) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        this.advanced = advanced;

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setBorder(new EmptyBorder(5,5,5,5));
        add(dialogPanel, BorderLayout.CENTER);

        if (inSandbox()) {
            JLabel noSave = new JLabel(getSandboxMessage());
            add(noSave, BorderLayout.NORTH);
        }

        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BorderLayout());

        // Cancel & OK buttons.
        Box vbox = Box.createVerticalBox();
        vbox.add(Box.createVerticalStrut(10));
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        JButton cancelButton = new JButton(LabelProvider.getLabel("CANCEL"));
        buttonBox.add(cancelButton);
        buttonBox.add(Box.createHorizontalStrut(6));
        okButton = new JButton(LabelProvider.getLabel("OK"));
        buttonBox.add(okButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        vbox.add(buttonBox);
        vbox.add(Box.createVerticalStrut(10));
        innerPanel.add(vbox, BorderLayout.SOUTH);

        // Center panel, to hold individual settings panels.
        settingsLayout = new CardLayout();
        settingsPanel = new JPanel(settingsLayout);
        innerPanel.add(settingsPanel, BorderLayout.CENTER);

        // Icon bar to invoke the individual settings.
        Box iconBar = Box.createVerticalBox();
        iconBar.setBorder(new LineBorder(Color.lightGray, 1, true));

        String firstPanel = addSettingsPanels(iconBar);

        iconBar.add(Box.createVerticalGlue());
        Dimension size = iconBar.getPreferredSize();
        size.width = BUTTONS_WIDTH;
        iconBar.setPreferredSize(size);

        dialogPanel.add(iconBar, BorderLayout.LINE_START);
        dialogPanel.add(innerPanel, BorderLayout.CENTER);

        cancelButton.addActionListener(this::cancelButtonHandler);
        okButton.addActionListener(this::okButtonHandler);

        // Listen for window closing, to tell settings panels to cancel.
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                settingsPanels.values().forEach(AbstractSettingsBase::onCancel);
            }
        });

        // Activate the first settings panel.
        onAction(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, firstPanel));

        addEscapeListener(this);
        setSize(BUTTONS_WIDTH + PANEL_WIDTH, DIALOG_HEIGHT);
        // For debugging sizing issues.
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.println(String.format("Size %dx%d", SettingsDialog.this.getWidth(), SettingsDialog.this.getHeight()));
//            }
//        });

    }

    protected abstract String addSettingsPanels(Box iconBar);

    /**
     * Makes one settings panel and adds it to the settings panel holder panel.
     * @param iconResource The name of the file with the icon for the settings button.
     * @param caption Caption for the settings button icon.
     * @param ctor Constructor for the panel. Takes a SettingsHelper parameter.
     * @return The button.
     */
    protected JComponent makeSettingsPanel(String iconResource,
        String caption,
        Function<SettingsHelper, AbstractSettingsBase> ctor)
    {
        ImageIcon icon = new ImageIcon(getResource(iconResource));
        LabelButton button = new LabelButton(icon);
        button.setToolTipText(caption);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setVerticalTextPosition(JLabel.BOTTOM);
        button.setHorizontalTextPosition(JLabel.CENTER);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setText("<html><div style='text-align:center'>" + caption + "</div></html>");
        button.setFont(LabelButton.getCustomFont(LabelButton.AVENIR, 13));
        button.setForeground(Color.darkGray);

        button.setActionCommand(caption);
        button.addActionListener(this::onAction);

        AbstractSettingsBase settingsPanel = ctor.apply(new SettingsHelper(button));
        settingsPanels.put(caption, settingsPanel);

        this.settingsPanel.add(caption, settingsPanel);

        return button;
    }

    /**
     * Handles the cancel button. Cancels (duh)
     * @param actionEvent is ignored.
     */
    private void cancelButtonHandler(ActionEvent actionEvent) {
        settingsPanels.values().forEach(AbstractSettingsBase::onCancel);
        this.setVisible(false);
    }

    /**
     * Handles the OK button. Notifies every panel to persist any changes. We assume that
     * it is OK to do so, because the button should not be enabled otherwise.
     * @param actionEvent is ignored.
     */
    private void okButtonHandler(ActionEvent actionEvent) {
        settingsPanels.values().forEach(AbstractSettingsBase::onOk);
        this.setVisible(false);
    }

    /**
     * The settings buttons invoke this. Contains the logic to switch between settings panels.
     * @param actionEvent We get the action command from the event.
     */
    private void onAction(ActionEvent actionEvent) {
        String actionCommand = actionEvent.getActionCommand();
        // Don't leave the current panel if it has invalid settings.
        if (currentTag != null && !settingsPanels.get(currentTag).settingsValid()) {
            return;
        }
        settingsLayout.show(settingsPanel, actionCommand);
        currentTag = actionCommand;
        setTitle(settingsPanels.get(currentTag).getTitle());
        okButton.setEnabled(settingsPanels.get(currentTag).settingsValid());
    }

    public abstract boolean inSandbox();

    public String getSandboxMessage() {
        return LabelProvider.getLabel("Running in 'Sandobox' mode; settings will not be saved.");
    }

}

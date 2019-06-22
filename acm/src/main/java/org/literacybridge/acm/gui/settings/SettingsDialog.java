package org.literacybridge.acm.gui.settings;

import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.literacybridge.acm.gui.UIConstants.getResource;
import static org.literacybridge.acm.utils.SwingUtils.getApplicationRelativeLocation;

/**
 * A dialog to implement a set of settings pages. Each page should be a logical grouping of
 * settings.
 */
public final class SettingsDialog extends JDialog {

    private static final String GENERAL = "General";
    private static final String VISIBLE_CATEGORIES = "Visible Categories";

    /**
     * Interface by which the settings panels can communicate with this dialog.
     */
    public interface SettingsHelper {
        void setValid(boolean valid);
    }
    private SettingsHelper settingsHelper = new SettingsHelper() {
        @Override
        public void setValid(boolean valid) {
            okButton.setEnabled(valid);
        }
    };

    // Width of the settings topics buttons, on the left side.
    private static final int BUTTONS_WIDTH = 100;
    // Width of the settings panels, on the right side.
    private static final int PANEL_WIDTH = 550;
    private static final int DIALOG_HEIGHT = 700;

    private final CardLayout settingsLayout;
    private final JPanel settingsPanel;
    private final JButton okButton;

    private Map<String, AbstractSettingsBase> settingsPanels = new LinkedHashMap<>();
    private String currentTag = null;

    private SettingsDialog(Window owner) {
        super(owner, "Settings", ModalityType.APPLICATION_MODAL);

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setBorder(new EmptyBorder(5,5,5,5));
        add(dialogPanel, BorderLayout.CENTER);

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

        iconBar.add(Box.createVerticalStrut(15));
        iconBar.add(makeSettingsPanel(UIConstants.GEARS_64_PNG, GENERAL, GeneralSettingsPanel::new));

        iconBar.add(Box.createVerticalStrut(15));
        iconBar.add(makeSettingsPanel(UIConstants.TREE_64_PNG, VISIBLE_CATEGORIES,
            VisibleCategoriesPanel::new));

        // If we need an import specific settings panel:
//        iconBar.add(Box.createVerticalStrut(15));
//        iconBar.add(makeSettingsPanel("usb_64.png", "Content Importer", EmptySettingsPanel::new));
//
        // If we need a deployment specific settings panel:
//        iconBar.add(Box.createVerticalStrut(15));
//        iconBar.add(makeSettingsPanel("tb_64g.png", "Deployment Creation", EmptySettingsPanel::new));
//
        // For a future "Languages in project" panel:
//        iconBar.add(Box.createVerticalStrut(15));
//        iconBar.add(makeSettingsPanel("language_64.png", "Languages", EmptySettingsPanel::new));

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
        onAction(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, GENERAL));

        setSize(BUTTONS_WIDTH + PANEL_WIDTH, DIALOG_HEIGHT);
        // For debugging sizing issues.
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.println(String.format("Size %dx%d", SettingsDialog.this.getWidth(), SettingsDialog.this.getHeight()));
//            }
//        });

    }

    /**
     * Makes one settings panel and adds it to the settings panel holder panel.
     * @param iconResource The name of the file with the icon for the settings button.
     * @param caption Caption for the settings button icon.
     * @param ctor Constructor for the panel. Takes a SettingsHelper parameter.
     * @return The button.
     */
    private JComponent makeSettingsPanel(String iconResource,
        String caption,
        Function<SettingsHelper, AbstractSettingsBase> ctor)
    {
        ImageIcon icon = new ImageIcon(getResource(iconResource));
        LabelButton button = new LabelButton(icon);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setVerticalTextPosition(JLabel.BOTTOM);
        button.setHorizontalTextPosition(JLabel.CENTER);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setText("<html><div style='text-align:center'>" + caption + "</div></html>");
        button.setFont(LabelButton.getCustomFont(LabelButton.AVENIR, 13));
        button.setForeground(Color.darkGray);

        button.setActionCommand(caption);
        button.addActionListener(this::onAction);

        AbstractSettingsBase settingsPanel = ctor.apply(settingsHelper);
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
        okButton.setEnabled(settingsPanels.get(currentTag).isValid());
    }

    /**
     * Constructs, positions, and shows the dialog.
     * @param e is ignored.
     */
    public static void showDialog(ActionEvent e) {
        SettingsDialog dialog = new SettingsDialog(Application.getApplication());
        // Place the new dialog within the application frame. This is hacky, but if it fails, the dialog
        // simply winds up in a funny place. Unfortunately, Swing only lets us get the location of a
        // component relative to its parent.
        Point pAudio = getApplicationRelativeLocation(Application.getApplication()
            .getMainView()
            .getAudioItemView());
        dialog.setLocation(pAudio);
        dialog.setVisible(true);
    }

}

package org.literacybridge.acm.gui.settings;

import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.UIConstants;

import javax.swing.*;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;

import static org.literacybridge.acm.utils.SwingUtils.getApplicationRelativeLocation;

/**
 * A dialog to implement a set of settings pages. Each page should be a logical grouping of
 * settings.
 */
public final class AcmSettingsDialog extends AbstractSettingsDialog {

    private static final String GENERAL = "General";
    private static final String VISIBLE_CATEGORIES = "Visible Categories";
    private static final String DESKTOP_SHORTCUTS = "Desktop Shortcuts";

    private AcmSettingsDialog(Window owner) {
        super(owner, "Settings");
    }

    @Override
    protected String addSettingsPanels(Box iconBar) {
        iconBar.add(Box.createVerticalStrut(15));
        iconBar.add(makeSettingsPanel(UIConstants.GEARS_64_PNG,
            GENERAL,
            AcmGeneralSettingsPanel::new));

        iconBar.add(Box.createVerticalStrut(15));
        iconBar.add(makeSettingsPanel(UIConstants.TREE_64_PNG,
            VISIBLE_CATEGORIES,
            VisibleCategoriesPanel::new));

        iconBar.add(Box.createVerticalStrut(15));
        iconBar.add(makeSettingsPanel(UIConstants.SHORTCUTS_64_PNG,
            DESKTOP_SHORTCUTS,
            DesktopShortcutsPanel::new));

        return GENERAL;
    }

    /**
     * Constructs, positions, and shows the dialog.
     *
     * @param e is ignored.
     */
    @SuppressWarnings("unused")
    public static void showDialog(ActionEvent e) {
        AcmSettingsDialog dialog = new AcmSettingsDialog(Application.getApplication());
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

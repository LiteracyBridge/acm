package org.literacybridge.acm.gui.settings;

import javax.swing.*;

public abstract class AbstractSettingsBase extends JPanel {
    protected AcmSettingsDialog.SettingsHelper helper;

    protected AbstractSettingsBase(AcmSettingsDialog.SettingsHelper helper) {
        super();
        this.helper = helper;
    }

    /**
     * Get the title by which the panel should be known.
     * @return The title.
     */
    public String getTitle() {return "Settings";}

    /**
     * Notify the panel that the user clicked Cancel, or closed the window. Panel should discard
     * settings.
     */
    public abstract void onCancel();

    /**
     * Notify the panel that the user clicked OK. Panel should persist settings.
     */
    public abstract void onOk();

    /**
     * Called to ask the panel if all of its settings are valid.
     * @return true if all are valid and ok to be saved. False if the values shouldn't be saved.
     */
    public boolean settingsValid() {return true;}
}

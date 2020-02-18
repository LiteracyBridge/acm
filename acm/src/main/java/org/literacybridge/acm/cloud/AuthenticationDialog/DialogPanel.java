package org.literacybridge.acm.cloud.AuthenticationDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

class DialogPanel extends JPanel {

    final DialogController dialogController;
    private final String dialogTitle;

    DialogPanel(DialogController dialogController, String dialogTitle) {
        this.dialogController = dialogController;
        this.dialogTitle = dialogTitle;

        addComponentListener(componentAdapter);
    }

    /**
     * Handles any actions that need to be taken when the panel is shown or hidden.
     */
    ComponentAdapter componentAdapter = new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent evt) {
            dialogController.setTitle(dialogTitle);
            onShown();
        }
    };

    void onShown() {
        // Override as needed
    }

    void onEnter() {
        // Override as needed
    }

    void onCancel(ActionEvent e) {
        // Override as needed
    }

}

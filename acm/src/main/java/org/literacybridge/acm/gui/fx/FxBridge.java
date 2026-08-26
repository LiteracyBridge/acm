package org.literacybridge.acm.gui.fx;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import javax.swing.SwingUtilities;

/**
 * Swing/JavaFX interop helpers, shared by every JavaFX screen we add while the UI is migrated
 * away from Swing.
 *
 * <p>Swing and JavaFX each own a thread -- the AWT event dispatch thread (EDT) and the JavaFX
 * application thread -- and a control may only be touched from its own. Everything here exists
 * to make crossing that line explicit at the call site.
 */
public final class FxBridge {
    private FxBridge() {}

    private static boolean toolkitStarted = false;

    /**
     * Creates the JFXPanel that hosts a JavaFX scene inside a Swing container. Constructing the
     * first JFXPanel is what boots the JavaFX toolkit, so this doubles as our initialization.
     *
     * <p>Must be called on the EDT.
     */
    public static JFXPanel createHostPanel() {
        JFXPanel host = new JFXPanel();
        if (!toolkitStarted) {
            toolkitStarted = true;
            // The JavaFX toolkit shuts down for good when the last FX window closes, and it can
            // not be restarted in the same process. Our FX scenes live inside Swing windows that
            // open and close repeatedly, so implicit exit has to be off.
            Platform.setImplicitExit(false);
        }
        return host;
    }

    /** Runs {@code task} on the JavaFX application thread. */
    public static void onFx(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    /** Runs {@code task} on the Swing event dispatch thread. */
    public static void onSwing(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}

package org.literacybridge.acm.gui.fx;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import javax.swing.SwingUtilities;

public final class FxBridge {
    private FxBridge() {}

    private static boolean toolkitStarted = false;

    /**
     * Starts the JavaFX toolkit without creating a JFXPanel, so callers that only ever show
     * JavaFX content in its own Stage never pull in javafx.swing (and therefore never need the
     * jdk.unsupported.desktop module JFXPanel unconditionally requires).
     *
     * <p>Safe to call even if some other, not-yet-migrated screen elsewhere in the app still
     * uses {@link #createHostPanel()} (JFXPanel) -- whichever of the two runs first wins, and
     * the other becomes a no-op.
     *
     * <p>Must be called on the EDT.
     */
    public static synchronized void startToolkit() {
        if (toolkitStarted) return;
        toolkitStarted = true;
        try {
            Platform.startup(() -> Platform.setImplicitExit(false));
        } catch (IllegalStateException alreadyRunning) {
            // A JFXPanel elsewhere already booted the toolkit first. Nothing more to do; just
            // make sure implicit exit is off, same as createHostPanel() below does.
            Platform.runLater(() -> Platform.setImplicitExit(false));
        }
    }

    /**
     * Creates the JFXPanel that hosts a JavaFX scene inside a Swing container.
     *
     * @deprecated Requires the jdk.unsupported.desktop JDK module at runtime (see
     * JDK-8210092 -- javafx.swing has no fallback that avoids this). Screens being newly
     * migrated should use {@link #startToolkit()} plus their own {@code javafx.stage.Stage}
     * instead, as {@code LoginCard} now does. Left in place only for screens not yet migrated
     * off JFXPanel.
     */
    @Deprecated
    public static JFXPanel createHostPanel() {
        JFXPanel host = new JFXPanel();
        if (!toolkitStarted) {
            toolkitStarted = true;
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
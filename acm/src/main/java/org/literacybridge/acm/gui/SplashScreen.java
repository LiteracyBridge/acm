package org.literacybridge.acm.gui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.literacybridge.acm.gui.SplashScreen.SPLASH_OPTIONS.ALWAYS_ON_TOP;
import static org.literacybridge.acm.gui.SplashScreen.SPLASH_OPTIONS.NOT_ON_TOP;

public class SplashScreen extends JFrame {
    public enum SPLASH_OPTIONS {
        ALWAYS_ON_TOP,
        NOT_ON_TOP
    }

    private final JLabel progressLabel = new JLabel("Starting...");

    public SplashScreen() {
        setLayout(new BorderLayout());
        setSize(500, 525);
        setAlwaysOnTop(true);
        setResizable(false);
        setUndecorated(true);

        try {
            Image resourceImage = ImageIO.read(UIConstants.getResource(UIConstants.SPLASH_SCREEN_IMAGE));
            ImageIcon imageIcon = new ImageIcon(resourceImage);
//        BufferedImage fileImage = ImageIO.read(new File(UIConstants.SPLASH_SCREEN_IMAGE));
//        ImageIcon imageIcon = new ImageIcon(fileImage);
            JLabel image = new JLabel(imageIcon);
            add(image, BorderLayout.NORTH);
        } catch (IOException e) {
            // ignore
        }

        progressLabel.setPreferredSize(new Dimension(500, 25));
        progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(progressLabel, BorderLayout.SOUTH);

        setOpacity(0.0f);
        setVisible(true);
    }

    public void showSplashScreen() {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2,
            dim.height / 2 - this.getSize().height / 2);
        toFront();
        // This looks like a redundant call to waitForUi(), but it really is necessary to avoid a blank dialog box.
        waitForUi();
        makeNonTransparent();
    }

    public void makeNonTransparent() {
        setOpacity(1.0f);
        waitForUi();
    }

    public void makeTransparent() {
        // Swing responds more quickly to setOpacity() than to setVisible(). I *think* because it keeps the layout
        // updated when the window is "visible" even though totally transparent.
        setOpacity(0.0f);
    }

    public void setProgressLabel(String text, SPLASH_OPTIONS... options) {
        Set<SPLASH_OPTIONS> splashOptions = new HashSet<>(Arrays.asList(options));
        if (splashOptions.contains(NOT_ON_TOP))
            this.setAlwaysOnTop(false);
        else if (splashOptions.contains(ALWAYS_ON_TOP))
            this.setAlwaysOnTop(true);

        this.progressLabel.setText(text);
        waitForUi();
    }

    /**
     * Does nothing but wait for the Swing dispatch queue to empty.
     */
    private void waitForUi() {
        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(() -> {
                });
            }
        } catch (InterruptedException | InvocationTargetException e) {
            // ignore
        }
    }
}

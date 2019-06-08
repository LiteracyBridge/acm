package org.literacybridge.acm.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;

public class SplashScreen extends JFrame {
  private JLabel progressLabel = new JLabel("Starting...");

  public SplashScreen() {
    setSize(500, 525);
    setResizable(false);
    setUndecorated(true);

    try {
      JLabel image = new JLabel(new ImageIcon(ImageIO
          .read(UIConstants.getResource(UIConstants.SPLASH_SCREEN_IMAGE))));
      add(image, BorderLayout.NORTH);
    } catch (IOException e) {
      // ignore
    }

    JProgressBar bar = new JProgressBar();
    progressLabel.setPreferredSize(new Dimension(500, 25));
    progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
    progressLabel.setVerticalAlignment(SwingConstants.CENTER);
    add(progressLabel, BorderLayout.SOUTH);
  }

  public void showSplashScreen() {
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    this.setLocation(dim.width / 2 - this.getSize().width / 2,
        dim.height / 2 - this.getSize().height / 2);
    pack();
    setVisible(true);
    toFront();
  }

  public void close() {
    setVisible(false);
  }

  public void setProgressLabel(String text) {
    this.progressLabel.setText(text);
  }
}

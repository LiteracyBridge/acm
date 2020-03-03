package org.literacybridge.acm.gui;

import java.awt.Dimension;

import javax.swing.*;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;

public class ACMStatusBar extends JXStatusBar {
  private JLabel statusLabel;

  private JLabel progressLabel;
  private JProgressBar progressBar;
  private JButton cancelButton;

  public ACMStatusBar() {
    putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
    setPreferredSize(new Dimension(1600, 25));

    statusLabel = new JLabel();
    add(statusLabel, new JXStatusBar.Constraint(400));

    add(new JSeparator(SwingConstants.VERTICAL));

    progressLabel = new JLabel("");
    add(progressLabel, new JXStatusBar.Constraint(300));

    progressBar = new JProgressBar();
    add(progressBar, new JXStatusBar.Constraint(200));

    cancelButton = new JButton("Cancel");
    add(cancelButton);

    progressBar.setVisible(false);
    progressLabel.setVisible(false);
    cancelButton.setVisible(false);
  }

  public void setStatusMessage(String message) {
    setStatusMessage(message, 0);
  }

  private Timer eraseTimer;
  public void setStatusMessage(String message, int diplayDurationMillis) {
    if (eraseTimer != null) {
      eraseTimer.stop();
      eraseTimer = null;
    }
    if (diplayDurationMillis > 0) {
      // We don't really need to create a new timer each time. But this is simpler, and it is very low volume.
      eraseTimer = new Timer(diplayDurationMillis, e->{eraseTimer.stop();eraseTimer=null;statusLabel.setText("");});
      eraseTimer.start();
    }
    statusLabel.setText(message);
  }


  public void setProgressMessage(String message) {
    progressLabel.setText(message);
  }

  public JButton getCancelButton() {
    return cancelButton;
  }

  public JProgressBar getProgressBar() {
    return progressBar;
  }

  public JLabel getProgressLabel() {
    return progressLabel;
  }

  public void startTask(String label) {
    progressLabel.setText(label);
    progressBar.setValue(0);
    progressBar.setVisible(true);
    progressLabel.setVisible(true);
    cancelButton.setVisible(true);
  }

  public void endTask() {
    progressBar.setVisible(false);
    progressLabel.setVisible(false);
    cancelButton.setVisible(false);
  }
}

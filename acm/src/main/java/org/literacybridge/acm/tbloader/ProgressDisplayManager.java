package org.literacybridge.acm.tbloader;

import org.literacybridge.core.tbloader.ProgressListener;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

class ProgressDisplayManager extends ProgressListener {
    private static final Logger LOG = Logger.getLogger(ProgressDisplayManager.class.getName());

    private final TbLoaderPanel tbLoaderPanel;
    Steps currentStep = Steps.ready;

    public ProgressDisplayManager(TbLoaderPanel tbLoaderPanel) {this.tbLoaderPanel = tbLoaderPanel;}

    void clear(String value) {
        tbLoaderPanel.statusCurrent.setText(value);
        tbLoaderPanel.statusFilename.setText("");
        clearLog();
    }

    public void clearLog() {
        tbLoaderPanel.statusLog.setText("");
        tbLoaderPanel.statusLog.setForeground(Color.BLACK);
    }

    void setStatus(String value) {
        tbLoaderPanel.statusCurrent.setText(value);
        tbLoaderPanel.statusFilename.setText("");
    }

    @Override
    public void step(Steps step) {
        currentStep = step;
        tbLoaderPanel.statusCurrent.setText(step.description());
        tbLoaderPanel.statusFilename.setText("");
        LOG.log(Level.INFO, "STEP: " + step.description());
    }

    @Override
    public void detail(String value) {
        tbLoaderPanel.statusFilename.setText(value);
        // Uncomment following line for more detailed logging
        //LOG.log(Level.INFO, "DETAIL: " + value);
    }

    @Override
    public void log(String value) {
        tbLoaderPanel.statusLog.setText(value + "\n" + tbLoaderPanel.statusLog.getText());
        LOG.log(Level.INFO, "PROGRESS: " + value);
    }

    @Override
    public void log(boolean append, String value) {
        if (!append) {
            log(value);
        } else {
            LOG.log(Level.INFO, "PROGRESS: " + value);
            String oldValue = tbLoaderPanel.statusLog.getText();
            int nl = oldValue.indexOf("\n");
            if (nl > 0) {
                String pref = oldValue.substring(0, nl);
                String suff = oldValue.substring(nl + 1);
                tbLoaderPanel.statusLog.setText(pref + value + "\n" + suff);
            } else {
                tbLoaderPanel.statusLog.setText(oldValue + value);
            }
        }
    }

    public void error(String value) {
        log(value);
        tbLoaderPanel.statusLog.setForeground(Color.RED);
        LOG.log(Level.SEVERE, "SEVERE: " + value);
    }

}

package org.literacybridge.acm.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

public class BackgroundTaskManager {
  private static final Logger LOG = Logger
      .getLogger(BackgroundTaskManager.class.getName());

  private final ACMStatusBar statusBar;
  private final Queue<ExtendedSwingWorker<?, ?>> tasks = new ArrayDeque<ExtendedSwingWorker<?, ?>>();
  private ExtendedSwingWorker<?, ?> active;

  public BackgroundTaskManager(ACMStatusBar statusBar) {
    this.statusBar = statusBar;
  }

  public abstract static class ExtendedSwingWorker<T, V> extends
      SwingWorker<T, V> implements ActionListener, PropertyChangeListener {
    private BackgroundTaskManager taskManager;

    @Override
    protected final void done() {
      onDone();
      taskManager.statusBar.endTask();
      taskManager.statusBar.getCancelButton().removeActionListener(this);
      taskManager.scheduleNext();
    }

    protected void onDone() {
      // nothing by default
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      cancel(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      if ("progress".equals(evt.getPropertyName())) {
        taskManager.statusBar.getProgressBar()
            .setValue((Integer) evt.getNewValue());
      }
    }
  }

  public synchronized void execute(final ExtendedSwingWorker<?, ?> worker) {
    worker.taskManager = this;
    tasks.offer(worker);
    if (active == null) {
      scheduleNext();
    }
  }

  protected synchronized void scheduleNext() {
    if ((active = tasks.poll()) != null) {
      statusBar.getCancelButton().addActionListener(active);
      statusBar.startTask(active.toString());

      active.execute();
    }
  }
}

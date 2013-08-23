package org.literacybridge.acm.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

public class BackgroundTaskManager {
	private static final Logger LOG = Logger.getLogger(BackgroundTaskManager.class.getName());
	
	private final ACMStatusBar statusBar;
	private final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
    private Runnable active;

    public BackgroundTaskManager(ACMStatusBar statusBar) {
    	this.statusBar = statusBar;
    }
    
    public synchronized void execute(final SwingWorker<?,?> worker) {
        tasks.offer(new MonitorThread(worker));
        if (active == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext() {
        if ((active = tasks.poll()) != null) {
            new Thread(active).start();
        }
    }
    
    private class MonitorThread implements Runnable, ActionListener {
    	private final SwingWorker<?,?> worker;

    	private MonitorThread(SwingWorker<?,?> worker) {
    		this.worker = worker;
    	}
    	
		@Override
		public void run() {
			try {
				statusBar.getCancelButton().addActionListener(this);
				statusBar.startTask(worker.toString());
				worker.execute();
				
				while (!worker.isDone() && !worker.isCancelled()) {
					try {
						statusBar.getProgressBar().setValue(worker.getProgress());
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			} finally {
				statusBar.endTask();
				statusBar.getCancelButton().removeActionListener(this);
				scheduleNext();
			}
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			worker.cancel(false);
		}

    }
}

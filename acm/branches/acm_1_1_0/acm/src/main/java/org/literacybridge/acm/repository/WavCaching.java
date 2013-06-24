package org.literacybridge.acm.repository;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.content.AudioItem;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;

public class WavCaching {

    JProgressBar progressBar;	
    private static HashSet<String> uncachedFiles = new HashSet<String>();

	public static void cacheNewA18Files() {
		findUncachedWaveFiles();
		if (uncachedFiles.size() > 0) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					int s = uncachedFiles.size();
					System.out.println("Setting progress bar max to: " + Integer.toString(s) );
					ConvertNewA18Files p = new ConvertNewA18Files(s);
					p.createAndShowGUI();
				}
			});
		}		
	}
	private static void findUncachedWaveFiles () {
	    String audioItemName;
		File repository = new File(Configuration.getRepositoryDirectory(),"org\\literacybridge");
		if (repository.listFiles() != null) {
		    for (File audioItem : repository.listFiles()) {
			    if (".".equals(audioItem.getName()) || "..".equals(audioItem.getName()) || audioItem.isFile()) {
				      continue;  // Ignore the self and parent aliases.
				    }
			    audioItemName = audioItem.getName();
			    File cachedItem = new File(Configuration.getCacheDirectory(),"org\\literacybridge\\"+audioItemName+"\\"+audioItemName+".wav");
			    
			    if (cachedItem.exists()) 
			    	System.out.print("found:");
			    else {
			    	System.out.print("not found:");
			    	uncachedFiles.add(audioItemName);
			    }
			    System.out.println(cachedItem.getAbsolutePath());
		    }
		}
	}
	
	@SuppressWarnings("serial")
	public static class ConvertNewA18Files extends JPanel implements ActionListener, PropertyChangeListener {
	
	private JProgressBar progressBar;
	private JButton cancelButton;
	private Task task;
	public int max;
	
	class Task extends SwingWorker<Void, Void> {
		/*
	* Main task. Executed in background thread.
	*/
		
		public Task () {
			//this.max = max;
		}
		@Override
		public Void doInBackground() {
			int progress = 0;
			//Initialize progress property.
			setProgress(0);
			System.out.println("total:"+uncachedFiles.size());
			Iterator<String> i = uncachedFiles.iterator();
		    while (i.hasNext()) {
				String audioItemName = i.next();
				AudioItem item = AudioItem.getFromDatabase(audioItemName);
				if (item != null) {
					System.out.println("Converting " + audioItemName);
					try {
						Configuration.getRepository().convert(item, AudioFormat.WAV);
					} catch (ConversionException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					// .a18 in repository but no entry in the database
					// not sure why this would happen, but for now - don't convert to wav.
					System.out.println("Will not convert " + audioItemName + "; not in DB.");
				}
		        setProgress(++progress);
			}
			return null;
		}
		
		/*
		* Executed in event dispatching thread
		*/
		@Override
		public void done() {
			cancelButton.setEnabled(true);
			setCursor(null); //turn off the wait cursor
//			taskOutput.append("Done!\n");
			
		}
	
	}
	
		public ConvertNewA18Files(int max) {
			super(new BorderLayout());
			this.max = max;
			//Create the demo's UI.
			cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("start");
			cancelButton.addActionListener(this);
			
			progressBar = new JProgressBar(0, max);
			progressBar.setValue(0);
			progressBar.setStringPainted(true);
			
//			taskOutput = new JTextArea(5, 20);
//			taskOutput.setMargin(new Insets(5,5,5,5));
//			taskOutput.setEditable(false);
//			
			JPanel panel = new JPanel();
			panel.add(cancelButton);
			panel.add(progressBar);
			
			add(panel, BorderLayout.PAGE_START);
//			add(new JScrollPane(taskOutput), BorderLayout.CENTER);
			setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		}
		
		/**
		* Invoked when the user presses the start button.
		*/
		public void actionPerformed(ActionEvent evt) {
			cancelButton.setEnabled(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			task.cancel(true);
		}
		
		/**
		* Invoked when task's progress property changes.
		*/
		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress" == evt.getPropertyName()) {
				int progress = (Integer) evt.getNewValue();
				progressBar.setValue(progress);
			} 
		}
		
		
		/**
		* Create the GUI and show it. As with all GUI code, this must run
		* on the event-dispatching thread.
		*/
		public void createAndShowGUI() {
			//Create and set up the window.
			JFrame frame = new JFrame("Converting New Files");
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			//Create and set up the content pane.
			JComponent newContentPane = this;
			newContentPane.setOpaque(true); //content panes must be opaque
			frame.setContentPane(newContentPane);
			
			task = new Task();
			frame.pack();
			frame.setVisible(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			task.addPropertyChangeListener(this);
			task.execute();	
		}
	
	}
	
}

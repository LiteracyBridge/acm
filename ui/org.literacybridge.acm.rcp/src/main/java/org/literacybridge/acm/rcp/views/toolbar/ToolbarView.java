package org.literacybridge.acm.rcp.views.toolbar;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISizeProvider;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.literacybridge.acm.rcp.core.Activator;

import org.literacybridge.acm.rcp.sound.PlayerStateDetails;
import org.literacybridge.acm.rcp.sound.SimpleSoundPlayer;

public class ToolbarView extends ViewPart implements ISizeProvider, Observer {

	private FormToolkit toolkit;
	private Form form;
	
	// Player
	private SimpleSoundPlayer player = new SimpleSoundPlayer();
	private Button leftBtn = null;
	private Button rightBtn = null;
	private Button playBtn = null;
	private Scale positionSlider = null;
	private PlayerStateDetails currPlayerDetails = null;	
	private double durtation = 0.0;
	private Label audioTitle = null;
	private Label playedTime = null;
	private Label remainingTime = null;
	private boolean scaleGrapped = false;
	
	Image imagePlay = Activator.getImageDescriptor("icons/play-24px.png").createImage();
	Image imageLeft = Activator.getImageDescriptor("icons/back-24px.png").createImage();
	Image rightPlay = Activator.getImageDescriptor("icons/forward-24px.png").createImage();
	Image imagePause = Activator.getImageDescriptor("icons/pause-24px.png").createImage();
	
	@Override
	public void createPartControl(Composite parent) {
		addPlayerControls(parent);
		player.addObserver(this);
		
		// testing
		String audioFile = "/Volumes/MAC_HOME/USERS/coder/Projects/talkingbook/acm/TestData/testWav.wav";
		initPlayer(audioFile);
	}

	private boolean initPlayer(String audioFilePath) {
		File audioFile = new File(audioFilePath);
    	player.setClip(audioFile);		
		return true;
	}

	private void addPlayerControls(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createForm(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 6;
		form.getBody().setLayout(layout);
		TableWrapData twd = null;
		
		// Add player buttons
		twd = new TableWrapData(TableWrapData.FILL);
		twd.colspan = 1;
		addPlayerButtons(form.getBody(), twd);
		
		// Add time, tile, ...
		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 4;
		addInfoControls(form.getBody(), twd);
		
		// Search box
		twd = new TableWrapData(TableWrapData.FILL);
		twd.colspan = 1;
		addSearchControls(form.getBody(), twd);
		
	}
	
	
	/**
	 * Add player buttons to parent.
	 * @param parent Composite.
	 */
	private void addSearchControls(Composite parent, TableWrapData twd) {
		// info
		Form form = toolkit.createForm(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 3;
		form.getBody().setLayout(layout);	
		form.setLayoutData(twd);
		
		twd = new TableWrapData(TableWrapData.FILL);
		twd.colspan = 1;
			
		Label search = toolkit.createLabel(form.getBody(), "Search:");
		search.setLayoutData(twd);

		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 2;
		Text searchStr = toolkit.createText(form.getBody(), "Hello World. Search me!", SWT.BORDER);
		searchStr.setLayoutData(twd);	
	}
	
	/**
	 * Add player buttons to parent.
	 * @param parent Composite.
	 */
	private void addInfoControls(Composite parent, TableWrapData twd) {
		// info
		Form form = toolkit.createForm(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 3;
		form.getBody().setLayout(layout);	
		form.setLayoutData(twd);
		
		// already played time
		twd = new TableWrapData(TableWrapData.LEFT);
		twd.colspan = 1;	
		playedTime = toolkit.createLabel(form.getBody(), "00:00");
		playedTime.setLayoutData(twd);
		
		// Title
		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 1;
		audioTitle = toolkit.createLabel(form.getBody(), "---");
		audioTitle.setLayoutData(twd);
		
		// remaining time
		twd = new TableWrapData(TableWrapData.RIGHT);
		twd.colspan = 1;	
		remainingTime = toolkit.createLabel(form.getBody(), "04:14");
		remainingTime.setLayoutData(twd);
		
		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 3;
			
		positionSlider = new Scale(form.getBody(), SWT.CENTER);
		positionSlider.setLayoutData(twd);		
		addSliderMovedListener(positionSlider);
	}
	

	/**
	 * Add player buttons to parent.
	 * @param parent Composite.
	 */
	private void addPlayerButtons(Composite parent, TableWrapData twd) {
		// info
		Form form = toolkit.createForm(parent);
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 4;
		form.getBody().setLayout(layout);	
		form.setLayoutData(twd);
		
		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 1;
			
		leftBtn = toolkit.createButton(form.getBody(), "", SWT.PUSH);
		leftBtn.setImage(imageLeft);
		leftBtn.setLayoutData(twd);

		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 2;
		
		playBtn = toolkit.createButton(form.getBody(), "", SWT.PUSH);
		playBtn.setImage(imagePlay);
		playBtn.setLayoutData(twd);
		
		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 1;
		
		rightBtn = toolkit.createButton(form.getBody(), "", SWT.PUSH);
		rightBtn.setImage(rightPlay);
		rightBtn.setLayoutData(twd);
		
		twd = new TableWrapData(TableWrapData.FILL);
		twd.colspan = 1;
		Label dummyLabel = toolkit.createLabel(form.getBody(), "");
		dummyLabel.setLayoutData(twd);
		
		// add listeners
		addPlayBtnListener();		   
	}
	
	private void addPlayBtnListener() {
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				if (event.widget == playBtn) {
					PlayerStateDetails psd = player.getPlayerStateDetails();
					if (psd.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.PAUSED) {
						player.play();
					} else if (psd.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
						player.stop();
					}
				}
			}
		};
		playBtn.addListener(SWT.Selection, listener);	
	}
	
	private void addSliderMovedListener(Scale slider) {
		slider.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				PlayerStateDetails psd = player.getPlayerStateDetails();
				// handle only if player is running
				if (psd.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
					int value = positionSlider.getSelection();
					player.play(value);
				}
				scaleGrapped = false;
			}			
		});
		
		slider.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				scaleGrapped = true;				
			}			
		});
		
		slider.addListener(SWT.MouseMove, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (player.getPlayerStateDetails().getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
					int value = positionSlider.getSelection();
					updatePlayerTimes(value);		
				}	    	
			}			
		});
	}
	
	
	@Override
	public void setFocus() {
	}
	
	@Override
	public int computePreferredSize(boolean width, int availableParallel,
			int availablePerpendicular, int preferredResult) {
		if (width == false) {
			return 70;
		}
		
		return computePreferredSize(width, availableParallel, availablePerpendicular, preferredResult);
	}

	@Override
	public int getSizeFlags(boolean width) {
		return SWT.MIN | SWT.MAX;
	}

	private void mirrorPlayerState(PlayerStateDetails newState) {
		currPlayerDetails = newState;
		if (currPlayerDetails.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.PAUSED) {
			playBtn.setImage(imagePlay);
		} else if (currPlayerDetails.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
			playBtn.setImage(imagePause);
	    	durtation = player.getDurationInSecs();
	    	
	    	// update only if scale is not moved by user
	    	if (!scaleGrapped) {
				positionSlider.setMaximum((int) durtation);
				positionSlider.setSelection((int) currPlayerDetails.getCurrentPoitionInSecs());	    		
	    	
				// Update label controls
				audioTitle.setText("Some Title");
				int playedTimeInSecs = (int) currPlayerDetails.getCurrentPoitionInSecs();
				playedTime.setText(secondsToTimeString(playedTimeInSecs));
				remainingTime.setText(secondsToTimeString((int) (durtation - playedTimeInSecs)));
	    	}
		}
	}
	
	private void updatePlayerTimes(int currPosInSecs) {
		playedTime.setText(secondsToTimeString(currPosInSecs));
		remainingTime.setText(secondsToTimeString((int) (durtation - currPosInSecs)));
	}

	private void ResetPlayerControls() {
		playedTime.setText(secondsToTimeString(0));
		remainingTime.setText(secondsToTimeString(0));
		scaleGrapped = false;
	}
	
	private String secondsToTimeString(int seconds) {
		final int SECONDS_PER_MINUTE = 60;
		return String.format("%d:%02d", 
				  seconds / SECONDS_PER_MINUTE, 
				  seconds % SECONDS_PER_MINUTE);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof PlayerStateDetails) {
			final PlayerStateDetails playerState = (PlayerStateDetails) arg;
			UIJob newJob = new UIJob("Player State") {			
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					mirrorPlayerState(playerState);
					return null;
				}
			};
			 
			newJob.schedule();
		}		
	}
}

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
import org.literacybridge.acm.rcp.sound.IPlayerStateListener;
import org.literacybridge.acm.rcp.sound.SimpleSoundPlayer;

public class ToolbarView extends ViewPart implements ISizeProvider, IPlayerStateListener, Observer {

	private FormToolkit toolkit;
	private Form form;
	
	// player
	private SimpleSoundPlayer player = new SimpleSoundPlayer();
	private Button leftBtn = null;
	private Button rightBtn = null;
	private Button playBtn = null;
	
	Image imagePlay = Activator.getDefault().getImageDescriptor("icons/play-24px.png").createImage();
	Image imageLeft = Activator.getDefault().getImageDescriptor("icons/back-24px.png").createImage();
	Image rightPlay = Activator.getDefault().getImageDescriptor("icons/forward-24px.png").createImage();
	Image imagePause = Activator.getDefault().getImageDescriptor("icons/pause-24px.png").createImage();

	
	
	@Override
	public void createPartControl(Composite parent) {
		addPlayerControls(parent);
		player.addObserver(this);
		
		// testing
		File audioFile = new File("/Volumes/MAC_HOME/USERS/coder/Projects/talkingbook/acm/TestData/testWav.wav");
    	player.setClip(audioFile);
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
		Label playedTime = toolkit.createLabel(form.getBody(), "00:00");
		playedTime.setLayoutData(twd);
		
		// Title
		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 1;
		Label audioTitle = toolkit.createLabel(form.getBody(), "---");
		audioTitle.setLayoutData(twd);
		
		// remaining time
		twd = new TableWrapData(TableWrapData.RIGHT);
		twd.colspan = 1;	
		Label remainingTime = toolkit.createLabel(form.getBody(), "04:14");
		remainingTime.setLayoutData(twd);
		
		twd = new TableWrapData(TableWrapData.FILL_GRAB);
		twd.colspan = 3;
			
		Scale positionSlider = new Scale(form.getBody(), SWT.CENTER);
		positionSlider.setMaximum(100);
		positionSlider.setLayoutData(twd);		
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
		
//		twd = new TableWrapData(TableWrapData.FILL);
//		twd.colspan = 2;
//			
//		Scale positionSlider = new Scale(form.getBody(), SWT.CENTER);
//		positionSlider.setMaximum(100);
//		positionSlider.setLayoutData(twd);	
//		
//		twd = new TableWrapData(TableWrapData.FILL);
//		twd.colspan = 1;
//		Label dummyLabel2 = toolkit.createLabel(form.getBody(), "");
//		dummyLabel2.setLayoutData(twd);
		
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				if (event.widget == playBtn) {
					PlayerState state = player.getPlayerState();
					if (state == PlayerState.STOPPED) {
						player.play();		
					} else if (state == PlayerState.RUNNING) {
						player.stop();
					}
				}
			}
		};

		playBtn.addListener(SWT.Selection, listener);
		   
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

	
	private void mirrorPlayerState(PlayerState newState) {
		if (newState == PlayerState.STOPPED | newState == PlayerState.STOPPED) {
			playBtn.setImage(imagePause);
		} else if (newState == PlayerState.RUNNING) {
			playBtn.setImage(imagePlay);
		}
	}


	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof PlayerState) {
			final PlayerState playerState = (PlayerState) arg;
			UIJob newJob = new UIJob("Device Message Bus") {
				
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

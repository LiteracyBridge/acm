package org.literacybridge.acm.player;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Observable;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.Time;

public class SimpleSoundPlayer extends Observable
             implements ISoundPlayer,
                        ControllerListener,
                        Runnable {
	
    private static final int LISTENER_UPDATE_TIME_PERIOD = 100;
	private Player player = null;
    private Thread playThread = null;
    private File currentClip;
    
    // details about player
	public static enum PlayerState { RUNNING, PAUSED };
    private PlayerState playerState = PlayerState.PAUSED;
    private double durationInSecs = 0.0;
     private double playFromSec = 0.0;
    
    /**
     * Constructor.
     */
    public SimpleSoundPlayer() {
    	resetPlayer();
    }
     
    /**
     * Initialize with an audio file.
     */
    public void setClip(File file) {
        currentClip = file;      
    	resetPlayer();
    }
    
    public void play() {
        if(player == null) {
            try {
                MediaLocator mediaLocator = new MediaLocator(currentClip.toURI().toURL());
                player = Manager.createPlayer(mediaLocator);
                player.addControllerListener(this);
                player.realize();
            }
            catch(MalformedURLException ex) {
                ex.printStackTrace();
            }
            catch(NoPlayerException ex) {
                ex.printStackTrace();
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }
        else {
            player.setMediaTime(new Time(playFromSec)); // set player position
            player.addControllerListener(this);
            player.start();
        }
        
        playerState = PlayerState.RUNNING;
        
        if (playThread == null) {
        	playThread = new Thread(this);           
        	playThread.start();
        }
    }
    
    public void play(double playFromSecond) {
    	stop(true);
    	playFromSec = playFromSecond;
    	play();
    }
    
    public void stop() {
    	stop(false);   
    }
    
    private void stop(boolean reposition) {
        if (player != null) {
            player.stop();
            player.removeControllerListener(this);
            if (!reposition) {
            	playFromSec = player.getMediaTime().getSeconds(); // save current player position
            }
        	// Update listeners
            playThread = null;
        	playerState = PlayerState.PAUSED;
            updatePlayerListeners(); // call explicit as player thread already interrupt
        }    
    }
    
    public void controllerUpdate(ControllerEvent ev) { 	
        if (ev instanceof RealizeCompleteEvent) {
            player.prefetch();
        }
        
        if (ev instanceof PrefetchCompleteEvent) {
            durationInSecs = player.getDuration().getSeconds();
        	playThread = new Thread(this);
            playThread.start();
            player.getGainControl().setLevel(1);           
            player.start();          
            playerState = PlayerState.RUNNING;
            updatePlayerListeners(); // call explicit to inform listeners immediately
        }
        
        if (ev instanceof EndOfMediaEvent) {
            player.removeControllerListener(this);
            player.stop();
            player.close();
            player = null;
            playThread = null;
            
            resetPlayer();
            updatePlayerListeners(); // call explicit as player thread already interrupt
        }
    }
    
    private void resetPlayer() {
        playerState = PlayerState.PAUSED;  	
        durationInSecs = 0.0;
        playFromSec = 0.0;
        if (player != null) {
            player.close();
            player = null;       	
        }
    }
    
    private double getCurrentTime() {
    	if (player != null) {
    		Time time = player.getMediaTime();
    	    return time.getSeconds();
    	}
    	
    	return 0.0;
    }
    
    public void run() {
    	// update listeners within defined time period
        while (playThread != null && !playThread.isInterrupted()) {
            updatePlayerListeners();

        	try {
                Thread.sleep(LISTENER_UPDATE_TIME_PERIOD);
            }
            catch(InterruptedException ex) {
            	break;
            }
       }
        
        updatePlayerListeners(); // JTBD
    }
    
    private void updatePlayerListeners() {
    	PlayerStateDetails currentDetails = getPlayerStateDetails();
    	setChanged();
    	notifyObservers(currentDetails);
    }

	public PlayerStateDetails getPlayerStateDetails() {
		return new PlayerStateDetails(playerState, getCurrentTime());
	}

	public double getDurationInSecs() {
		return durationInSecs;
	}
}

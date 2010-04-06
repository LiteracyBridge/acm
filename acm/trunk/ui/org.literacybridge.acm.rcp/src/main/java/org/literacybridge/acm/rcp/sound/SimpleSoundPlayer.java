package org.literacybridge.acm.rcp.sound;

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

import org.literacybridge.acm.rcp.sound.IPlayerStateListener.PlayerState;

public class SimpleSoundPlayer extends Observable
             implements ISoundPlayer,
                        ControllerListener,
                        Runnable {
    private Player player = null;
    private Thread playThread = null;
    private File currentClip;
    private PlayerState playerState = PlayerState.STOPPED;
    
    public SimpleSoundPlayer() {
    }
    
    
    public void setClip(File file) {
        currentClip = file;
    }
    
    public void play() {
        if(player == null) {
            try {
                MediaLocator mediaLocator = new MediaLocator(currentClip.toURI().toURL());
                player = Manager.createPlayer(mediaLocator);
                player.addControllerListener(this);
                player.realize();
                internalPlayerStateChanged(playerState.RUNNING);
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
            player.start();
            internalPlayerStateChanged(PlayerState.RUNNING);
        }
    }
    
    public void pause() {
        if(player != null) {
            player.stop();
            internalPlayerStateChanged(PlayerState.PAUSED);
        }
    }
    public void stop() {
        if(player != null) {
            player.removeControllerListener(this);
            player.stop();
            player.close();
            player = null;
        }
        
        internalPlayerStateChanged(PlayerState.STOPPED);
    }
    
    public void controllerUpdate(ControllerEvent ev) {
        if(ev instanceof RealizeCompleteEvent) {
            player.prefetch();
        }
        if(ev instanceof PrefetchCompleteEvent) {
            playThread = new Thread(this);
            playThread.start();
            player.getGainControl().setLevel(1);
            player.start();
        }
        if(ev instanceof EndOfMediaEvent) {
            player.removeControllerListener(this);
            player.stop();
            player.close();
            player = null;
            if(playThread != null) {
                playThread = null;
            }
            internalPlayerStateChanged(PlayerState.STOPPED);
        }
    }
    
    public double getCurrentTime() {
    	if (player != null) {
    		Time time = player.getMediaTime();
    	    return time.getSeconds();
    	}
    	
    	return 0.0;
    }
    
    public void run() {
        while(playThread != null) {
            if(player != null) {

                try {
                    playThread.sleep(10);
                }
                catch(InterruptedException ex) {}
            }
       }
    }
    
    private void internalPlayerStateChanged(PlayerState newState) {
    	playerState = newState;
    	setChanged();
    	System.out.println("Current State: " + newState.toString());
    	notifyObservers(newState);
    }
    
	public PlayerState getPlayerState() {
		return playerState;
	}
}

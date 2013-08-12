package org.literacybridge.acm.sound;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.media.*;
import javax.swing.JProgressBar;

public class SimpleSoundPlayer
             implements SoundPlayer,
                        ControllerListener,
                        Runnable {
    private Player player = null;
    private JProgressBar progressBar = new JProgressBar();
    private Thread playThread = null;
    private File currentClip;
    
    public SimpleSoundPlayer() {
    }
    
    public void setProgressBar(JProgressBar bar) {
        progressBar = bar;
    }
    
    public void setClip(File file) {
        currentClip = file;
    }
    
    public void play() {
        if(player == null) {
            try {
                MediaLocator mediaLocator = new MediaLocator(currentClip.toURL());
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
            player.start();
        }
    }
    
    public void pause() {
        if(player != null) {
            player.stop();
        }
    }
    public void stop() {
        if(player != null) {
            player.removeControllerListener(this);
            player.stop();
            player.close();
            player = null;
        }
        progressBar.setValue(0);
        progressBar.setString("");
    }
    
    public void controllerUpdate(ControllerEvent ev) {
        if(ev instanceof RealizeCompleteEvent) {
            player.prefetch();
        }
        if(ev instanceof PrefetchCompleteEvent) {
            Time time = player.getDuration();
            progressBar.setMaximum((int) time.getSeconds());
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
            progressBar.setValue(0);
        }
    }
    
    public void run() {
        while(playThread != null) {
            if(player != null) {
                Time time = player.getMediaTime();
                progressBar.setValue((int) time.getSeconds());
                try {
                    playThread.sleep(10);
                }
                catch(InterruptedException ex) {}
            }
        }
    }
}

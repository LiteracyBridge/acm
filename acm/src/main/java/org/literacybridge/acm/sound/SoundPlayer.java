package org.literacybridge.acm.sound;

import java.io.File;

import javax.swing.JProgressBar;

public interface SoundPlayer {
   
    public void setProgressBar(JProgressBar bar);
    
    public void setClip(File file);
    
    public void play();    

    public void pause();

    public void stop();    
}

package org.literacybridge.acm.rcp.sound;

import java.io.File;

public interface ISoundPlayer {
   
	// Set file to play
    public void setClip(File file);
    
    public void play();    
    public void pause();
    public void stop(); 
}

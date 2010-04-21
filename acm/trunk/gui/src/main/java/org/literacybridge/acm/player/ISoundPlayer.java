package org.literacybridge.acm.player;

import java.io.File;

public interface ISoundPlayer {
   
	// Set file to play
    public void setClip(File file);
    
    public void play();    
    public void stop(); 
}

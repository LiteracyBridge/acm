package org.literacybridge.acm.gui.playerAPI;

import java.io.File;

public interface ISoundPlayer {

    /**
     * Plays the given file from the beginning.
     *
     * @param audioFile to be played.
     */
    void play(File audioFile);

    /**
     * Plays the given file from the given location.
     *
     * @param audioFile     to be played.
     * @param startPosition Time in seconds at which playback should start.
     */
    void play(File audioFile, double startPosition);

    /**
     * Seek to the given location in the file.
     *
     * @param seconds Time in seconds at which to continue playback.
     */
    void seek(double seconds);

    /**
     * Pauses playing.
     */
    void pause();

    /**
     * Resums playing when paused.
     */
    void resume();

    /**
     * Stops playing and resets the player.
     */
    void stop();

}

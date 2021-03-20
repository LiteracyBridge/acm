package org.literacybridge.acm.gui.playerAPI;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
        implements ISoundPlayer, ControllerListener {

    private Player player = null;
    private File currentClip;

    // details about player
    public enum PlayerState {
        NOT_PLAYING, PLAYING, PAUSED;

        /**
         * Is audio currently playing?
         *
         * @return true if playing (not paused).
         */
        public boolean isPlaying() {
            return this == PLAYING;
        }

        /**
         * Is the audio currently NOT playing?
         *
         * @return true if stopped or paused.
         */
        public boolean isNotPlaying() {
            return this != PLAYING;
        }

        /**
         * Has the player been started, but is currently paused?
         *
         * @return true if player is paused (neither playing, nor fully stopped).
         */
        public boolean isPaused() {
            return this == PAUSED;
        }

        /**
         * Has audio been started? Even if currently paused.
         *
         * @return true if audio has been started. It may be currently paused.
         */
        public boolean isStarted() {
            return this != NOT_PLAYING;
        }

        /**
         * Is the player completely inactive?
         *
         * @return true if there is no current audio file.
         */
        public boolean isStopped() {
            return this == NOT_PLAYING;
        }
    }

    private PlayerState playerState = PlayerState.NOT_PLAYING;
    private double durationInSecs = 0.0;
    private double playFromSec = 0.0;

    /**
     * Constructor.
     */
    public SimpleSoundPlayer() {
        doReset();
    }

    /**
     * Plays the given file from the beginning.
     *
     * @param audioFile to be played.
     */
    @Override
    public void play(File audioFile) {
        play(audioFile, 0.0);
    }

    /**
     * Plays the given file from the given location.
     *
     * @param audioFile     to be played.
     * @param startPosition Time in seconds at which playback should start.
     */
    @Override
    public void play(File audioFile, double startPosition) {
        log("op: play(%s), state: %s", audioFile.getName(), playerState);
        doReset();
        currentClip = audioFile;
        playFromSec = startPosition;
        doPlay();
        updatePlayerListeners();
    }

    /**
     * Pauses playback.
     */
    @Override
    public void pause() {
        log("op: pause, state: %s", playerState);
        if (playerState == PlayerState.PLAYING) {
            doPause();
            playFromSec = getCurrentTime();
        }
        updatePlayerListeners();
    }

    /**
     * Resumes playback from a paused state.
     */
    @Override
    public void resume() {
        log("op: resume, state: %s", playerState);
        if (playerState == PlayerState.PAUSED) {
            doPlay();
        }
        updatePlayerListeners();
    }

    /**
     * Seek to the given location in the file.
     *
     * @param seconds Time in seconds at which to continue playback.
     */
    @Override
    public void seek(double seconds) {
        log("op: seek(%f), state: %s", seconds, playerState);
        if (playerState == PlayerState.PLAYING) {
            doPause();
            playFromSec = seconds;
            doPlay();
        } else if (playerState == PlayerState.PAUSED) {
            playFromSec = seconds;
        }
        updatePlayerListeners();
    }

    /**
     * Stops playback. Clears the player.
     */
    @Override
    public void stop() {
        log("op: stop, state: %s", playerState);
        doReset();
        updatePlayerListeners();
    }

    /**
     * Starts or resumes playing the current message.
     * <p>
     * The player runs on a different thread, so we must be careful about which value of player is being
     * manipulated, hence the synchronized methods.
     * <p>
     * Empirically, this method, controllerUpdate, and resetPlayer need to be synchronized, and _pause
     * must NOT be (causes hangs).
     *
     * @return true if the playback started, false otherwise.
     */
    private synchronized boolean doPlay() {
        if (player == null) {
            try {
                if (currentClip != null) {
                    log("play, new player, state: %s", playerState);
                    MediaLocator mediaLocator = new MediaLocator(
                            currentClip.toURI().toURL());
                    player = Manager.createPlayer(mediaLocator);
                    player.addControllerListener(this);
                    player.realize();
                } else {
                    return false;
                }
            } catch (NoPlayerException | IOException ex) {
                ex.printStackTrace();
                return false;
            }
        } else {
            log("play, existing player, state: %s", playerState);
            player.setMediaTime(new Time(playFromSec)); // set player position
            player.addControllerListener(this);
            player.start();
        }

        playerState = PlayerState.PLAYING;

        return true;
    }

    private void doPause() {
        log("pause, state: %s", playerState);
        if (player != null) {
            player.stop();
            player.removeControllerListener(this);
        }
        playerState = PlayerState.PAUSED;
    }

    private synchronized void doReset() {
        log("resetPlayer, state: %s", playerState);
        playerState = PlayerState.NOT_PLAYING;
        durationInSecs = 0.0;
        playFromSec = 0.0;
        if (player != null) {
            player.stop();
            player.close();
            player = null;
        }
    }

    @Override
    public synchronized void controllerUpdate(ControllerEvent ev) {
        String eventName = ev.getClass().toString();
        eventName = eventName.substring(eventName.lastIndexOf('.'));
        log("controllerUpdate: %s, state: %s", eventName, playerState);

        if (ev instanceof RealizeCompleteEvent) {
            if (player != null) {
                player.prefetch();
            }
        }

        if (ev instanceof PrefetchCompleteEvent) {
            if (player != null) {
                durationInSecs = player.getDuration().getSeconds();
                player.start();
                playerState = PlayerState.PLAYING;
                updatePlayerListeners();
            }
        }

        if (ev instanceof EndOfMediaEvent) {
            if (player != null) {
                player.removeControllerListener(this);
            }
            doReset();
            updatePlayerListeners();
        }
    }

    private double getCurrentTime() {
        if (player != null) {
            Time time = player.getMediaTime();
            return time.getSeconds();
        }

        return 0.0;
    }

    private void updatePlayerListeners() {
        PlayerStateDetails currentDetails = getPlayerStateDetails();
        setChanged();
        notifyObservers(currentDetails);
    }

    /**
     * Retrieves the current state of the player.
     *
     * @return a PlayerStateDetails object describing the current state.
     */
    public PlayerStateDetails getPlayerStateDetails() {
        double currentTime = 0;
        if (playerState == PlayerState.PLAYING) {
            currentTime = getCurrentTime();
        } else if (playerState == PlayerState.PAUSED) {
            currentTime = playFromSec;
        }
        return new PlayerStateDetails(playerState, currentTime);
    }

    /**
     * Retrieves the duration of the audio file, as determined from the player when the prefetch completed.
     *
     * @return The track duration, in seconds.
     */
    public double getDurationInSecs() {
        return durationInSecs;
    }

    // Set to true to log player activity.
    // TODO: Hook up to some UI for managing these debugging activities.
    public boolean isLogging = false;
    private static final DateFormat ISO8601time = new SimpleDateFormat("HHmmss.SSS'Z'",
            Locale.US); // Quoted "Z" to indicate UTC, no timezone offset

    private void log(String format, Object... args) {
        if (isLogging) {
            System.out.print(ISO8601time.format(new Date(System.currentTimeMillis())) + ": ");
            System.out.printf((format) + "%n", args);
        }
    }
}

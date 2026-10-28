package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.shape.SVGPath;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.PathsProvider;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.ApplicationV2;
import org.literacybridge.acm.gui.playerAPI.PlayerStateDetails;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class PlayerBarController {
    private static final String PLAY_ICON = "M8 5v14l11-7z";
    private static final String PAUSE_ICON = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";

    private final Timer updatePlayerStateTimer = new Timer(100, this::onUpdateTimerTick);
    private DashboardController dashboard;

    @FXML
    private Label titleLabel;

    @FXML
    private Label elapsedLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Slider positionSlider;

    @FXML
    private ToggleButton playPauseButton;

    @FXML
    private SVGPath playPauseIcon;

    @FXML
    private void initialize() {
//        this.player = Application.getApplication().getSoundPlayer();

        playPauseButton.selectedProperty().addListener(
                (observable, wasPlaying, playing) -> showPlaying(playing));
        positionSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> elapsedLabel.setText(formatTime(newValue.intValue())));

        // Dummy data until the dashboard is wired to the sound player.
        showTrack("Family Planning", 123);
        positionSlider.setValue(20);
        playPauseButton.setSelected(true);
    }

    public void showTrack(String title, int durationSeconds) {
        titleLabel.setText(title);
        totalLabel.setText(formatTime(durationSeconds));
        positionSlider.setMax(durationSeconds);
        positionSlider.setValue(0);
    }

    private void showPlaying(boolean playing) {
        String action = playing ? "Pause" : "Play";
        playPauseIcon.setContent(playing ? PAUSE_ICON : PLAY_ICON);
        playPauseButton.setAccessibleText(action);
        playPauseButton.getTooltip().setText(action);
    }

    static String formatTime(int totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    public void setDashboardController(DashboardController parentController) {
        this.dashboard = parentController;
    }

    public void playAudio(AudioItem item) {
        SimpleSoundPlayer player = getPlayer();
        if (player != null) {
            player.stop();
            updatePlayerStateTimer.stop();
        }

        // In cases where the file is locked, we can't play it.
        if (PathsProvider.getLocalLockFile(item.getId(), false).exists()) {
            // TODO: show error dialog/message/toast
            return;
        }

        System.out.println("Audio Item to play:" + item.getId());
        Application parent = Application.getApplication();
        try {
            // convert on the fly if necessary
//            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            File f = ACMConfiguration.getInstance().getCurrentDB().getRepository()
                    .getAudioFile(item, AudioItemRepository.AudioFormat.WAV);

//            Application.getFilterState().updateResult();
//            initPlayer();
            // TODO: update slider state
            player.play(f);
            updatePlayerStateTimer.start();
            // This is a bit hacky. At this point we know that the slider has been layed out, so we can
            // get it's real size, and apply that to the label.
//            Dimension d = sliderBox.getSize();
//            d.width -= 20;
//            titleInfoLbl.setMaximumSize(d);
//            titleInfoLbl.setPreferredSize(d);
//            titleInfoLbl.setText(item.getMetadata()
//                    .getMetadataValue(MetadataSpecification.DC_TITLE).getValue());
        } catch (IOException | BaseAudioConverter.ConversionException |
                 AudioItemRepository.UnsupportedFormatException e) {
            // There really isn't anything to do with this exception.
//      throw new RuntimeException(e);
        }
//        finally {
//            parent.setCursor(Cursor.getDefaultCursor());
//        }
    }

    private void onUpdateTimerTick(ActionEvent e) {
        PlayerStateDetails details = getPlayer().getPlayerStateDetails();
        mirrorPlayerState(details);
        // TODO: update slider state
    }

    private void mirrorPlayerState(PlayerStateDetails newState) {
//        if (newState.getCurrentPlayerState().isNotPlaying()) {
//            playBtn.setIcon(playImageIcon);
//            playBtn.setToolTipText(LabelProvider.getLabel("Play"));
//            boolean isPaused = newState.getCurrentPlayerState().isPaused();
//            stopBtn.setEnabled(isPaused);
//            positionSlider.setEnabled(isPaused);
//            if (newState.getCurrentPlayerState().isStopped()) {
//                titleInfoLbl.setText(NOT_PLAYING);
//                playedTimeLbl.setText(NO_TIME);
//                remainingTimeLbl.setText(NO_TIME);
//                positionSlider.setValue(0);
//            } else {
//                positionSlider.setMaximum((int) (duration * SLIDER_UNITS_PER_SECOND));
//                positionSlider.setValue((int) (newState.getCurrentPoitionInSecs() * SLIDER_UNITS_PER_SECOND));
//
//                int playedTimeInSecs = (int) newState.getCurrentPoitionInSecs();
//                playedTimeLbl.setText(secondsToTimeString(playedTimeInSecs));
//                remainingTimeLbl.setText(secondsToTimeString((int) (duration - playedTimeInSecs)));
//            }
//            updatePlayerStateTimer.stop();
//
//        } else if (newState.getCurrentPlayerState().isPlaying()) {
//            playBtn.setIcon(pauseImageIcon);
//            playBtn.setToolTipText(LabelProvider.getLabel("Pause"));
//            duration = player.getDurationInSecs();
//            stopBtn.setEnabled(true);
//            positionSlider.setEnabled(true);
//
//            // update slider only if slide is not being moved by user
//            if (!positionSliderGrabbed) {
//                positionSlider.setMaximum((int) (duration * SLIDER_UNITS_PER_SECOND));
//                positionSlider.setValue((int) (newState.getCurrentPoitionInSecs() * SLIDER_UNITS_PER_SECOND));
//
//                int playedTimeInSecs = (int) newState.getCurrentPoitionInSecs();
//                playedTimeLbl.setText(secondsToTimeString(playedTimeInSecs));
//                remainingTimeLbl.setText(secondsToTimeString((int) (duration - playedTimeInSecs)));
//            }
//            updatePlayerStateTimer.start();
//
//        }
    }

    private SimpleSoundPlayer getPlayer() {
        return ApplicationV2.getApplication().getSoundPlayer();
    }
}

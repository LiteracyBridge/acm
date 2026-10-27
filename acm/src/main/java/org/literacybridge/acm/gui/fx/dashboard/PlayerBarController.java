package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.shape.SVGPath;

public class PlayerBarController {
    private static final String PLAY_ICON = "M8 5v14l11-7z";
    private static final String PAUSE_ICON = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";

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
}

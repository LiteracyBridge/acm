package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.PathsProvider;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.playerAPI.PlayerStateDetails;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class DashboardController implements Initializable {
    @FXML
    private PlayerBarController playerBarController;

    @FXML
    private AudioLibraryController audioLibraryController;

    @FXML
    private SidebarController sidebarController;

    @FXML
    private TopBarController topBarController;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (playerBarController != null) {
            playerBarController.setDashboardController(this);
        }
        if (audioLibraryController != null) {
            audioLibraryController.setDashboardController(this);
        }
    }

    public PlayerBarController getPlayerBar() {
        return playerBarController;
    }

    public AudioLibraryController getAudioLibrary() {
        return audioLibraryController;
    }

}
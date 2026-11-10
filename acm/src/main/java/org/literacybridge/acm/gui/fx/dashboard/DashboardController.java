package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
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
import javafx.css.PseudoClass;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class DashboardController implements Initializable {
    private static final String PLACEHOLDER_FXML = "/org/literacybridge/acm/gui/dashboard/placeholderpage.fxml";
    private static final String AUDIO_LIBRARY_PAGE = "audioLibrary";
    private static final String CREATE_DEPLOYMENT_FXML = "/org/literacybridge/acm/gui/dashboard/createdeployment.fxml";
    private static final String CREATE_DEPLOYMENT_PAGE = "createDeployments";
    private static final PseudoClass FULL_BLEED = PseudoClass.getPseudoClass("full-bleed");

    private static final Map<String, String> PLACEHOLDER_TITLES = new HashMap<>();
    static {
        PLACEHOLDER_TITLES.put("importContent", "Import Content");
        PLACEHOLDER_TITLES.put("playlistPrompts", "Playlists Prompts");
        PLACEHOLDER_TITLES.put("systemPrompts", "System Prompts");
        PLACEHOLDER_TITLES.put("customGreetings", "Custom Greetings");
    }
    @FXML
    private PlayerBarController playerBarController;

    @FXML
    private AudioLibraryController audioLibraryController;

    @FXML
    private SidebarController sidebarController;

    @FXML
    private TopBarController topBarController;

    @FXML
    private StackPane contentArea;

    @FXML
    private Node audioLibrary;

    @FXML
    private Node playerBar;

    private final Map<String, Node> placeholderPages = new HashMap<>();
    private Node createDeploymentPage;
    private CreateDeploymentController createDeploymentController;
    private boolean onCreateDeploymentPage;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (playerBarController != null) {
            playerBarController.setDashboardController(this);
        }
        if (audioLibraryController != null) {
            audioLibraryController.setDashboardController(this);
        }
        if (sidebarController != null) {
            sidebarController.setOnNavigate(this::showPage);
        }
        if (topBarController != null && sidebarController != null) {
            topBarController.setOnMenuToggle(sidebarController::toggleCollapsed);
        }
    }

    public PlayerBarController getPlayerBar() {
        return playerBarController;
    }

    public AudioLibraryController getAudioLibrary() {
        return audioLibraryController;
    }

    private void showPage(String pageId) {
        boolean createDeployment = CREATE_DEPLOYMENT_PAGE.equals(pageId);
        Node page = createDeployment ? createDeploymentPage()
                : AUDIO_LIBRARY_PAGE.equals(pageId) ? audioLibrary
                : placeholderPage(pageId);
        // This page's white header runs edge to edge, so it skips the usual page padding.
        contentArea.pseudoClassStateChanged(FULL_BLEED, createDeployment);
        // The design for this page has no playlists, and shows the player bar only while a track plays.
        onCreateDeploymentPage = createDeployment;
        updatePlayerBarVisibility();
        sidebarController.setPlaylistsAvailable(!createDeployment);
        contentArea.getChildren().setAll(page);
    }

    private void updatePlayerBarVisibility() {
        boolean show = !onCreateDeploymentPage
                || createDeploymentController.playingRowProperty().get() != null;
        playerBar.setVisible(show);
        playerBar.setManaged(show);
    }

    private Node createDeploymentPage() {
        if (createDeploymentPage == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(CREATE_DEPLOYMENT_FXML));
            try {
                createDeploymentPage = loader.load();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            createDeploymentController = loader.getController();
            createDeploymentController.playingRowProperty().addListener((observable, oldRow, newRow) -> {
                // Dummy: shows the name and length only; no audio plays yet.
                if (newRow != null) {
                    playerBarController.showTrack(newRow.getName(), toSeconds(newRow.getDuration()));
                }
                updatePlayerBarVisibility();
            });
        }
        return createDeploymentPage;
    }

    // "mm:ss" to seconds.
    private static int toSeconds(String duration) {
        String[] parts = duration.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    // Loaded once per page id, then reused.
    private Node placeholderPage(String pageId) {
        return placeholderPages.computeIfAbsent(pageId, id -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(PLACEHOLDER_FXML));
            try {
                Node page = loader.load();
                Label title = (Label) loader.getNamespace().get("titleLabel");
                title.setText(PLACEHOLDER_TITLES.getOrDefault(id, id));
                return page;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }


}
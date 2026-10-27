package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleGroup;

import java.util.function.Consumer;

public class SidebarController {
    @FXML
    private ToggleGroup navGroup;
    @FXML
    private ListView<String> playlistList;

    private Consumer<String> onNavigate;

    @FXML
    private void initialize() {
        // Keep one nav item selected at all times; clicking the active one must not clear it.
        navGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                navGroup.selectToggle(oldToggle);
            } else if (onNavigate != null) {
                onNavigate.accept((String) newToggle.getUserData());
            }
        });

        // Dummy data until the dashboard is wired to the content store.
        playlistList.getItems().setAll(
                "Cowpea Storage",
                "Reasons for Handwashing",
                "Trust Parents",
                "kwonsa umwana amabere...");
    }

    // Set by the dashboard; receives the page id from the selected nav item's userData.
    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }
}
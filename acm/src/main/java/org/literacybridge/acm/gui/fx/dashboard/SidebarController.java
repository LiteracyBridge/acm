package org.literacybridge.acm.gui.fx.dashboard;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SidebarController {
    private static final PseudoClass COLLAPSED = PseudoClass.getPseudoClass("collapsed");

    @FXML
    private VBox sidebarRoot;
    @FXML
    private ToggleGroup navGroup;
    @FXML
    private Node playlistDivider;
    @FXML
    private Node playlistHeader;
    @FXML
    private ListView<String> playlistList;

    private Consumer<String> onNavigate;
    private boolean collapsed;
    private boolean playlistsAvailable = true;
    // Nav items given a tooltip only while collapsed; removed again on expand.
    private final List<ToggleButton> collapsedTooltipOwners = new ArrayList<>();

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

    // Some pages have no playlists in their design.
    public void setPlaylistsAvailable(boolean available) {
        playlistsAvailable = available;
        updatePlaylistsVisibility();
    }

    private void updatePlaylistsVisibility() {
        boolean visible = playlistsAvailable && !collapsed;
        for (Node node : new Node[] {playlistDivider, playlistHeader, playlistList}) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    public void toggleCollapsed() {
        setCollapsed(!collapsed);
    }

    // Collapsed: icon-only nav items, playlists hidden. Sizing lives in dashboard.css under .sidebar:collapsed.
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        sidebarRoot.pseudoClassStateChanged(COLLAPSED, collapsed);

        updatePlaylistsVisibility();

        // Without their text, nav items need a tooltip to say what they are.
        // Items that already have their own tooltip are left alone.
        if (collapsed) {
            for (Toggle toggle : navGroup.getToggles()) {
                ToggleButton item = (ToggleButton) toggle;
                if (item.getTooltip() == null) {
                    item.setTooltip(new Tooltip(item.getText()));
                    collapsedTooltipOwners.add(item);
                }
            }
        } else {
            collapsedTooltipOwners.forEach(item -> item.setTooltip(null));
            collapsedTooltipOwners.clear();
        }
    }
}
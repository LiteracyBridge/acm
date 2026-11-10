package org.literacybridge.acm.gui.fx.dashboard;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.Arrays;
import java.util.List;

public class ModificationController {
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass TRACK = PseudoClass.getPseudoClass("track");
    private static final PseudoClass PLAYING = PseudoClass.getPseudoClass("playing");
    private static final String SORT_UP_ICON = "M0 5L5 0L10 5Z";
    private static final String SORT_DOWN_ICON = "M0 0L5 5L10 0Z";
    // The 56px column header in dashboard.css, plus its divider line.
    private static final double HEADER_HEIGHT = 58;
    // Must match -fx-cell-size of the rows in dashboard.css.
    private static final double ROW_HEIGHT = 56;
    private static final double EMPTY_TABLE_HEIGHT = 200;

    @FXML
    private TreeTableView<ModificationRow> playlistTable;
    @FXML
    private TreeTableColumn<ModificationRow, Void> testAudioColumn;
    @FXML
    private ToggleButton publishToggle;
    @FXML
    private CheckBox tutorialCheckBox;
    @FXML
    private CheckBox feedbackPublicCheckBox;

    // The playlist or track that is playing; null when nothing is playing.
    private final ObjectProperty<ModificationRow> playingRow = new SimpleObjectProperty<>();

    @FXML
    private void initialize() {
        // Hidden root: playlists are its children, and tracks are the playlists' children.
        TreeItem<ModificationRow> root = new TreeItem<>();
        root.setExpanded(true);
        playlistTable.setRoot(root);

        // Track rows and the playing row get their own look in dashboard.css.
        playlistTable.setRowFactory(table -> new TreeTableRow<ModificationRow>() {
            @Override
            protected void updateItem(ModificationRow item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(TRACK, !empty && item != null && item.isTrack());
                pseudoClassStateChanged(PLAYING, !empty && item != null && item == playingRow.get());
            }
        });

        testAudioColumn.setCellFactory(column -> new TestAudioCell());
        playingRow.addListener((observable, oldRow, newRow) -> playlistTable.refresh());

        installSortIndicators();

        // Dummy data until the backend calls showPlaylists() with the real deployment.
        showPlaylists(dummyPlaylists());
    }

    // Each playlist row carries its own tracks; they become the playlist's child rows.
    public void showPlaylists(List<ModificationRow> playlists) {
        TreeItem<ModificationRow> root = playlistTable.getRoot();
        root.getChildren().clear();
        for (ModificationRow playlist : playlists) {
            TreeItem<ModificationRow> playlistItem = new TreeItem<>(playlist);
            playlistItem.expandedProperty().addListener((observable, wasExpanded, isExpanded) -> updateTableHeight());
            for (ModificationRow track : playlist.getTrackRows()) {
                playlistItem.getChildren().add(new TreeItem<>(track));
            }
            root.getChildren().add(playlistItem);
        }
        updateTableHeight();
    }

    // The backend and the player bar can watch this to play the audio.
    public ObjectProperty<ModificationRow> playingRowProperty() {
        return playingRow;
    }

    // On when the deployment should be published; off by default.
    public BooleanProperty publishProperty() {
        return publishToggle.selectedProperty();
    }

    // Ticked by default.
    public BooleanProperty includeTutorialProperty() {
        return tutorialCheckBox.selectedProperty();
    }

    // Not ticked by default.
    public BooleanProperty feedbackPublicProperty() {
        return feedbackPublicCheckBox.selectedProperty();
    }

    // The table grows with its rows, so the page scrolls rather than the table.
    private void updateTableHeight() {
        int rows = 0;
        for (TreeItem<ModificationRow> playlist : playlistTable.getRoot().getChildren()) {
            rows += 1 + (playlist.isExpanded() ? playlist.getChildren().size() : 0);
        }
        playlistTable.setPrefHeight(Math.max(EMPTY_TABLE_HEIGHT, HEADER_HEIGHT + rows * ROW_HEIGHT + 2)); // + 2 for the table border
    }

    private static List<ModificationRow> dummyPlaylists() {
        return Arrays.asList(
                ModificationRow.playlist("Talking Book", "40:35", Arrays.asList(
                        ModificationRow.track("Overview of the Talking Book", "14:02"),
                        ModificationRow.track("More about the Talking Book", "02:35"),
                        ModificationRow.track("Key Features", "20:35"),
                        ModificationRow.track("Key Benefits", "04:35"))),
                ModificationRow.playlist("Impact", "5:35", Arrays.asList(
                        ModificationRow.track("Impact Stories", "5:35"))),
                ModificationRow.playlist("Health Impact", "10:05", Arrays.asList(
                        ModificationRow.track("Health Impact Stories", "10:05"))),
                ModificationRow.playlist("Agricultural Impact", "18:05", Arrays.asList(
                        ModificationRow.track("Agricultural Impact Stories", "18:05"))));
    }

    // "Play Playlist" or "Play Track" button; it says "Playing..." while that row plays.
    private class TestAudioCell extends TreeTableCell<ModificationRow, Void> {
        private final Button button = new Button();

        TestAudioCell() {
            button.getStyleClass().add("btn-test-audio");
            // Clicking the playing row again stops it.
            button.setOnAction(event -> {
                ModificationRow row = getTreeTableRow().getItem();
                playingRow.set(row == playingRow.get() ? null : row);
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            ModificationRow row = empty ? null : getTreeTableRow().getItem();
            if (row == null) {
                setGraphic(null);
                return;
            }
            boolean playing = row == playingRow.get();
            button.setText(playing ? "Playing..." : row.isTrack() ? "Play Track" : "Play Playlist");
            button.pseudoClassStateChanged(TRACK, row.isTrack());
            button.pseudoClassStateChanged(PLAYING, playing);
            setGraphic(button);
        }
    }

    // Up/down arrows on every column, as in the design. The sorted direction is darkened.
    private void installSortIndicators() {
        for (TreeTableColumn<ModificationRow, ?> column : playlistTable.getColumns()) {
            VBox indicator = new VBox(2, sortTriangle(SORT_UP_ICON), sortTriangle(SORT_DOWN_ICON));
            column.setGraphic(indicator);
            column.sortTypeProperty().addListener((observable, oldType, newType) -> updateSortIndicators());
        }
        playlistTable.getSortOrder().addListener(
                (ListChangeListener<TreeTableColumn<ModificationRow, ?>>) change -> updateSortIndicators());
    }

    private void updateSortIndicators() {
        TreeTableColumn<ModificationRow, ?> sortedColumn =
                playlistTable.getSortOrder().isEmpty() ? null : playlistTable.getSortOrder().get(0);
        for (TreeTableColumn<ModificationRow, ?> column : playlistTable.getColumns()) {
            VBox indicator = (VBox) column.getGraphic();
            boolean sorted = column == sortedColumn;
            boolean ascending = column.getSortType() == TreeTableColumn.SortType.ASCENDING;
            indicator.getChildren().get(0).pseudoClassStateChanged(ACTIVE, sorted && ascending);
            indicator.getChildren().get(1).pseudoClassStateChanged(ACTIVE, sorted && !ascending);
        }
    }

    private static SVGPath sortTriangle(String content) {
        SVGPath triangle = new SVGPath();
        triangle.setContent(content);
        triangle.getStyleClass().add("sort-triangle");
        return triangle;
    }
}

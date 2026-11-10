package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.shape.SVGPath;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.store.MetadataStore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TextField;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.layout.VBox;

import javax.swing.*;
import java.awt.*;
import java.util.stream.Stream;

public class AudioLibraryController {
    private static final String MORE_ICON = "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final String SORT_UP_ICON = "M0 5L5 0L10 5Z";
    private static final String SORT_DOWN_ICON = "M0 0L5 5L10 0Z";

    private final SimpleSoundPlayer player = new SimpleSoundPlayer();
    private DashboardController dashboard;

    @FXML
    private TableView<AudioLibraryRow> audioTable;

    @FXML
    private TableColumn<AudioLibraryRow, Void> actionsColumn;

    @FXML
    private TextField searchField;

    private final ObservableList<AudioLibraryRow> allRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        actionsColumn.setCellFactory(column -> new RowMenuCell());
        installSortIndicators();

        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        Stream<AudioLibraryRow> audioItems = store.getAudioItems().stream().map(item -> {
            return new AudioLibraryRow(item);

        });
        allRows.setAll(audioItems.toArray(AudioLibraryRow[]::new));

        // Search narrows the rows; clicking a column header still sorts what is left.
        FilteredList<AudioLibraryRow> filteredRows = new FilteredList<>(allRows);
        searchField.textProperty().addListener((observable, oldText, newText) ->
                filteredRows.setPredicate(row -> row.matches(newText)));
        SortedList<AudioLibraryRow> sortedRows = new SortedList<>(filteredRows);
        sortedRows.comparatorProperty().bind(audioTable.comparatorProperty());
        audioTable.setItems(sortedRows);

        // Attach double click even listener to the table rows
        audioTable.setRowFactory(tv -> {
            TableRow<AudioLibraryRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    AudioLibraryRow rowData = row.getItem();

                    System.out.println(rowData);
                    dashboard.getPlayerBar().playAudio(rowData.getAudioItem());
                }
            });
            return row;
        });
    }

    public void setDashboardController(DashboardController parentController) {
        this.dashboard = parentController;
    }

    public TableView<AudioLibraryRow> getTable(){
        return audioTable;
    }

    private void installSortIndicators() {
        for (TableColumn<AudioLibraryRow, ?> column : audioTable.getColumns()) {
            if (!column.isSortable()) {
                continue;
            }
            VBox indicator = new VBox(2, sortTriangle(SORT_UP_ICON), sortTriangle(SORT_DOWN_ICON));
            column.setGraphic(indicator);
            column.sortTypeProperty().addListener((observable, oldType, newType) -> updateSortIndicators());
        }
        audioTable.getSortOrder().addListener(
                (ListChangeListener<TableColumn<AudioLibraryRow, ?>>) change -> updateSortIndicators());
    }

    private void updateSortIndicators() {
        TableColumn<AudioLibraryRow, ?> sortedColumn =
                audioTable.getSortOrder().isEmpty() ? null : audioTable.getSortOrder().get(0);
        for (TableColumn<AudioLibraryRow, ?> column : audioTable.getColumns()) {
            if (!(column.getGraphic() instanceof VBox)) {
                continue;
            }
            VBox indicator = (VBox) column.getGraphic();
            boolean sorted = column == sortedColumn;
            boolean ascending = column.getSortType() == TableColumn.SortType.ASCENDING;
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
    
    // The "more" button at the end of each row. Its menu comes in a later step.
    private static class RowMenuCell extends TableCell<AudioLibraryRow, Void> {
        private final Button button = new Button();

        RowMenuCell() {
            SVGPath icon = new SVGPath();
            icon.setContent(MORE_ICON);
            icon.getStyleClass().add("icon");
            button.setGraphic(icon);
            button.setAccessibleText("More actions");
            button.getStyleClass().add("icon-button");
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : button);
        }
    }
}

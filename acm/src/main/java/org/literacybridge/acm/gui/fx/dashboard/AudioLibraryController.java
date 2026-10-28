package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;

import java.util.Collection;
import java.util.stream.Stream;

public class AudioLibraryController {
    private static final String MORE_ICON = "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";

    @FXML
    private TableView<AudioLibraryRow> audioTable;
    @FXML
    private TableColumn<AudioLibraryRow, Void> actionsColumn;

    @FXML
    private void initialize() {
        actionsColumn.setCellFactory(column -> new RowMenuCell());

        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        Stream<AudioLibraryRow> audioItems = store.getAudioItems().stream().map(item -> {
            return new AudioLibraryRow(
                    item.getId(),
                    item.getTitle(),
                    item.getDuration(),
                    "Agriculture",
                    "Unknown",
                    item.getLanguageCode(),
                    "2024-09-21"
            );

        });
        audioTable.getItems().setAll(audioItems.toArray(AudioLibraryRow[]::new));

        // Attach double click even listener to the table rows
        audioTable.setRowFactory(tv -> {
            TableRow<AudioLibraryRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    AudioLibraryRow rowData = row.getItem();
                    System.out.println(rowData);
                }
            });
            return row;
        });
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
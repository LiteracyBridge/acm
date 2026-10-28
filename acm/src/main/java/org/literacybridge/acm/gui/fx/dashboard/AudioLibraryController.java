package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

//        audioTable.getItems().setAll(
//
//                new AudioLibraryRow("Fertilizer & Manure: Proper Use and Storage", "10:27", "Agriculture", "Unknown", "Dagaare", "2024-09-21"),
//                new AudioLibraryRow("Family Planning", "02:03", "Antenatal Care", "GHS-Wa Reg", "Dagaare", "2024-09-21"),
//                new AudioLibraryRow("Cowpea Storage", "08:32", "Storing", "LBG-Wa", "French", "2024-09-21"),
//                new AudioLibraryRow("Reasons for Handwashing", "02:17", "Handwashing", "Unknown", "Nepali", "2024-09-21"),
//                new AudioLibraryRow("Trust Parents", "01:47", "Gen Education", "Unknown", "Nepali", "2024-09-21"),
//                new AudioLibraryRow("kwonsa umwana amabere", "00:45", "Breastfeeding", "Unknown", "Kirundi", "2024-09-21"));
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
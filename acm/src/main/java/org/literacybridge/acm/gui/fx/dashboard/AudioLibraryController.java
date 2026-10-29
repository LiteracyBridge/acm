package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.shape.SVGPath;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.PathsProvider;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.messages.PlayAudioItemMessage;
import org.literacybridge.acm.gui.playerAPI.PlayerStateDetails;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

public class AudioLibraryController {
    private static final String MORE_ICON = "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";

    private final SimpleSoundPlayer player = new SimpleSoundPlayer();
    //    private final Timer updatePlayerStateTimer = new Timer(100, this::onUpdateTimerTick);
    private DashboardController dashboard;

    @FXML
    private TableView<AudioLibraryRow> audioTable;

    @FXML
    private TableColumn<AudioLibraryRow, Void> actionsColumn;

    @FXML
    private void initialize() {
        actionsColumn.setCellFactory(column -> new RowMenuCell());

        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        Stream<AudioLibraryRow> audioItems = store.getAudioItems().stream().map(item -> {
            return new AudioLibraryRow(item);

        });
        audioTable.getItems().setAll(audioItems.toArray(AudioLibraryRow[]::new));

        // Attach double click even listener to the table rows
        audioTable.setRowFactory(tv -> {
            TableRow<AudioLibraryRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    AudioLibraryRow rowData = row.getItem();

//                    PlayAudioItemMessage newMsg = new PlayAudioItemMessage(rowData.getAudioItem());
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
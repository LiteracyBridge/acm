package org.literacybridge.acm.gui.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class FxHelpers {
    public enum Scenes {
        PROGRAM_SELECTION("/org/literacybridge/acm/gui/programselection.fxml"),
        DASHBOARD("/org/literacybridge/acm/gui/dashboard/dashboard.fxml");

        private final String fxml;

        private Scenes(String fxmlPath) {
            this.fxml = fxmlPath;
        }

        public String getFxml() {
            return this.fxml;
        }
    }

    /**
     * Navigates the user to a new scene
     */
    public static void changeScene(Stage stage, Scenes scence ) throws IOException {
        FXMLLoader loader = new FXMLLoader(FxHelpers.class.getResource(scence.getFxml()));
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }

    public static void confirmationDialog(Stage stage, String title, String content, Consumer<Boolean> callback) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.initOwner(stage);

        Optional<ButtonType> result = alert.showAndWait();
        callback.accept(result.isPresent() && result.get() == ButtonType.YES);
    }

    public static void errorDailog(Stage stage, String title, String content, Boolean exitApp) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.initOwner(stage);

        if (exitApp) {
            System.exit(1);
        }
    }


}

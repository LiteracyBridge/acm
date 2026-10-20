package org.literacybridge.acm.gui.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.function.Consumer;

public class DailogHelpers {
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

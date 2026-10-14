package org.literacybridge.acm.gui.fx;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class FxLoginDialog {

    private boolean submitted;
    private String email;
    private String password;
    private boolean rememberMeSelected;

    // what it does:
    // loads fxml from resources
    // build the screen and controller
    public void showAndWait() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/loginscreen.fxml")
        );

        Parent root = loader.load();

        Stage stage = new Stage();
        LoginController controller = loader.getController();
        controller.setOnLogin(() -> {
            submitted = true;
            email = controller.getEmail();
            password = controller.getPassword();
            rememberMeSelected = controller.isRememberMeSelected();
            stage.close();
        });
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Amplio Login");
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }
}

package org.literacybridge.acm.gui.fx.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

public class TopBarController {
    @FXML
    private ComboBox<String> programComboBox;
    @FXML
    private Button avatarButton;

    private Runnable onMenuToggle;

    @FXML
    private void initialize() {
        // Dummy data until the dashboard is wired to the Authenticator.
        programComboBox.getItems().setAll("UNICEF Ghana", "CARE Ghana", "TEST");
        programComboBox.getSelectionModel().selectFirst();
        setUserName("Preview User");
    }

    public void setUserName(String name) {
        String initial = (name == null || name.trim().isEmpty())
                ? "?"
                : name.trim().substring(0, 1).toUpperCase();
        avatarButton.setText(initial);
    }

    // Set by the dashboard to collapse and expand the sidebar.
    public void setOnMenuToggle(Runnable onMenuToggle) {
        this.onMenuToggle = onMenuToggle;
    }

    @FXML
    private void handleMenuToggle() {
        if (onMenuToggle != null) {
            onMenuToggle.run();
        }
    }
}

package org.literacybridge.acm.gui.fx.dashboard;

import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;

public class CreateDeploymentController {
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass DONE = PseudoClass.getPseudoClass("done");

    @FXML
    private HBox step1;
    @FXML
    private HBox step2;
    @FXML
    private HBox step3;
    @FXML
    private VBox step1Content;
    @FXML
    private VBox step2Content;
    @FXML
    private VBox step3Content;
    @FXML
    private HBox stepButtons;
    @FXML
    private ComboBox<String> deploymentComboBox;
    @FXML
    private ComboBox<String> platformComboBox;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    // Controller of the included deploymentmodification.fxml (fx:id="modification").
    @FXML
    private ModificationController modificationController;

    private List<HBox> steps;
    private List<VBox> contents;
    private int currentStep;

    @FXML
    private void initialize() {
        steps = Arrays.asList(step1, step2, step3);
        contents = Arrays.asList(step1Content, step2Content, step3Content);

        // Dummy data until the page is wired to the program spec.
        deploymentComboBox.getItems().setAll("Deployment 1", "Deployment 2", "Deployment 3");
        platformComboBox.getItems().setAll("Companion App", "Talking Book");

        for (ComboBox<String> comboBox : Arrays.asList(deploymentComboBox, platformComboBox)) {
            showPromptWhenEmpty(comboBox);
            comboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateButtons());
        }

        showStep(0);
    }

    // Without this, the prompt text does not come back after the selection is cleared.
    private static void showPromptWhenEmpty(ComboBox<String> comboBox) {
        comboBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? comboBox.getPromptText() : item);
            }
        });
    }

    // The playlist or track playing in the Modification step; null when nothing is playing.
    public ObjectProperty<ModificationRow> playingRowProperty() {
        return modificationController.playingRowProperty();
    }

    private void showStep(int step) {
        currentStep = step;
        // Leaving the Modification step stops whatever was playing there.
        if (contents.get(step) != step2Content) {
            playingRowProperty().set(null);
        }
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).pseudoClassStateChanged(ACTIVE, i == step);
            steps.get(i).pseudoClassStateChanged(DONE, i < step);
            contents.get(i).setVisible(i == step);
            contents.get(i).setManaged(i == step);
        }
        // One row of buttons, moved under whichever step is open.
        VBox content = contents.get(step);
        if (stepButtons.getParent() != content) {
            content.getChildren().add(stepButtons);
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean stepOneComplete = deploymentComboBox.getValue() != null && platformComboBox.getValue() != null;
        previousButton.setDisable(currentStep == 0);
        nextButton.setDisable(currentStep == steps.size() - 1 || !stepOneComplete);
    }

    @FXML
    private void handlePrevious() {
        showStep(currentStep - 1);
    }

    @FXML
    private void handleNext() {
        showStep(currentStep + 1);
    }

    @FXML
    private void handleCancel() {
        deploymentComboBox.setValue(null);
        platformComboBox.setValue(null);
        showStep(0);
    }
}
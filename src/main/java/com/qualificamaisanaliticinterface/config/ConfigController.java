package com.qualificamaisanaliticinterface.config;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ConfigController {
    @FXML
    private DialogPane configPane;
    @FXML
    private TextField apiUrlField;
    @FXML
    private TextField supersetUrlField;
    @FXML
    private Label configError;
    @FXML
    private Button saveButton;

    private final ConnectionVerifier verifier;

    public ConfigController() {
        this(new ConnectionVerifier());
    }

    ConfigController(ConnectionVerifier verifier) {
        this.verifier = verifier;
    }

    @FXML
    private void initialize() {
        AppConfig.ConnectionAddresses addresses = AppConfig.configuredAddresses();
        apiUrlField.setPromptText(addresses.api());
        supersetUrlField.setPromptText(addresses.superset());
    }

    @FXML
    private void onSaveClick() {
        Stage stage = (Stage) configPane.getScene().getWindow();
        String apiInput = apiUrlField.getText();
        String supersetInput = supersetUrlField.getText();
        if (apiInput.isBlank() && supersetInput.isBlank()) {
            stage.hide();
            return;
        }

        AppConfig.ConnectionAddresses changes;
        try {
            changes = AppConfig.resolveConnectionChanges(apiInput, supersetInput);
        } catch (IllegalArgumentException exception) {
            showFeedback(exception.getMessage(), true, stage);
            return;
        }

        setChecking(true);
        showFeedback("Testando conexão…", false, stage);
        verifier.verify(changes).whenComplete((errors, failure) -> Platform.runLater(() -> {
            if (!stage.isShowing()) {
                return;
            }
            setChecking(false);
            if (failure != null) {
                showFeedback("Não foi possível concluir o teste de conexão. Tente novamente.", true, stage);
                return;
            }
            if (!errors.isEmpty()) {
                showFeedback(String.join("\n", errors), true, stage);
                return;
            }
            try {
                AppConfig.saveConnections(changes);
                stage.hide();
            } catch (IOException | IllegalArgumentException exception) {
                showFeedback("Não foi possível salvar os endereços. Tente novamente.", true, stage);
            }
        }));
    }

    private void setChecking(boolean checking) {
        saveButton.setDisable(checking);
        apiUrlField.setDisable(checking);
        supersetUrlField.setDisable(checking);
    }

    private void showFeedback(String message, boolean error, Stage stage) {
        configError.setText(message);
        configError.getStyleClass().removeAll("config-description", "config-error");
        configError.getStyleClass().add(error ? "config-error" : "config-description");
        configError.setManaged(true);
        configError.setVisible(true);
        stage.sizeToScene();
    }
}

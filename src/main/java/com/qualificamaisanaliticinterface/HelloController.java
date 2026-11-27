package com.qualificamaisanaliticinterface;

import com.qualificamaisanaliticinterface.features.login.AuthService;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.session.UserSession;

import com.qualificamaisanaliticinterface.session.SessionService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class HelloController {

    private final AuthService authService;
    private final SessionService sessionService;

    public HelloController() {
        this(new AuthService(), new SessionService());
    }

    public HelloController(AuthService authService, SessionService sessionService) {
        this.authService = authService;
        this.sessionService = sessionService;
    }

    private boolean loginInProgress;
    private boolean passwordVisibility;
    @FXML
    private TextField userField;
    @FXML
    private PasswordField passwordField;
    @FXML
    public TextField visiblePasswordField;
    @FXML
    private Label loginErrorLabel;
    @FXML
    private CheckBox persistLoginCheckBox;

    @FXML
    private void initialize() {
        visiblePasswordField.textProperty()
                .bindBidirectional(passwordField.textProperty());
    }

    @FXML
    private void onConfigClick() throws IOException {
        Navigator.openForm(userField.getScene(), "/views/config.fxml");
    }

    @FXML
    private void onClick() {
        if (loginInProgress) {
            return;
        }

        clearError();

        String username = userField.getText().strip();
        String password = passwordField.getText();

        if (username.isBlank()) {
            showError("Preencha o usuário.");
            return;
        } else if (password.isBlank()){
            showError("Preencha a senha.");
            return;
        }

        loginInProgress = true;

        authService.login(username, password)
                .whenComplete((response, failure) ->
                        Platform.runLater(() -> {
                            loginInProgress = false;

                            if (failure != null) {
                                Throwable cause = failure;

                                while (cause instanceof CompletionException
                                        && cause.getCause() != null) {
                                    cause = cause.getCause();
                                }

                                if (cause instanceof AuthService.LoginException) {
                                    showError(cause.getMessage());
                                } else {
                                    showError(
                                            "Não foi possível conectar à API. "
                                                    + "Confira a conexão e tente novamente."
                                    );
                                }

                                return;
                            }

                            try {
                                if (persistLoginCheckBox.isSelected()) {
                                    sessionService.saveSession(response);
                                } else {
                                    sessionService.clearSession();
                                }
                            } catch (IOException | IllegalArgumentException failureToSave) {
                                showError("Login aceito, mas não foi possível atualizar a sessão salva. Tente novamente.");
                                return;
                            }
                            UserSession.start(response);
                            passwordField.clear();

                            try {
                                showHome();
                            } catch (IOException exception) {
                                UserSession.clear();

                                showError(
                                        "Login aceito, mas não foi possível "
                                                + "abrir a tela inicial."
                                );
                            }
                        })
                );
    }

    protected void showHome() throws IOException {
        Stage stage = (Stage) userField.getScene().getWindow();
        Navigator.changeScreen(userField.getScene(), "/views/home.fxml");
        stage.setWidth(1380);
        stage.setHeight(810);
        stage.centerOnScreen();
    }

    private void showError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setManaged(true);
        loginErrorLabel.setVisible(true);
        growWindowToFitContent();
    }

    private void growWindowToFitContent() {
        Scene scene = loginErrorLabel.getScene();
        if (scene == null || !(scene.getWindow() instanceof Stage stage)
                || !stage.isShowing() || stage.isMaximized() || stage.isFullScreen()) {
            return;
        }

        Parent root = scene.getRoot();
        root.applyCss();
        double decorationHeight = stage.getHeight() - scene.getHeight();
        double requiredHeight = Math.ceil(root.prefHeight(scene.getWidth()) + decorationHeight);
        if (requiredHeight > stage.getHeight()) {
            stage.setHeight(requiredHeight);
        }
    }

    private void clearError() {
        loginErrorLabel.setText("");
        loginErrorLabel.setVisible(false);
        loginErrorLabel.setManaged(false);
    }

    public void onTogglePassword(ActionEvent actionEvent) {
        if(passwordVisibility){
            passwordField.setText(visiblePasswordField.getText());
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibility = false;
        } else {
            visiblePasswordField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            passwordVisibility = true;
        }
    }
}

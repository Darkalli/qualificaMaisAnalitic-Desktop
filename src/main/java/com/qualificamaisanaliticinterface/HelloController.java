package com.qualificamaisanaliticinterface;

import com.qualificamaisanaliticinterface.features.login.AuthService;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.session.UserSession;

import com.qualificamaisanaliticinterface.session.WindowsTokenStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class HelloController {

    private final AuthService authService = new AuthService();

    private boolean loginInProgress;

    @FXML
    private TextField userField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label loginErrorLabel;

    @FXML
    private CheckBox persistLoginCheckBox;

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

                            UserSession.start(response);
                            if(persistLoginCheckBox.isSelected()){
                                WindowsTokenStore store = new WindowsTokenStore();

                                try {
                                    store.save(response.token());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            passwordField.clear();

                            try {
                                Navigator.changeScreen(
                                        userField.getScene(),
                                        "/views/home.fxml"
                                );
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

    private void showError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setManaged(true);
        loginErrorLabel.setVisible(true);
    }

    private void clearError() {
        loginErrorLabel.setText("");
        loginErrorLabel.setVisible(false);
        loginErrorLabel.setManaged(false);
    }
}
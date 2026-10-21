package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.features.login.AuthService;
import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.session.UserSession;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;


import java.io.IOException;

public class HomeController {

    @FXML
    public BorderPane workspace;
    @FXML
    public Label registersCount;

    private final AuthService authService = new AuthService();

    private boolean logoutInProgress;

    @FXML
    private void onLogoutClick(ActionEvent event) {
        if (logoutInProgress) {
            return;
        }
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        Parent loginRoot;
        try {
            loginRoot = FXMLLoader.load(
                    HomeController.class.getResource("/views/login.fxml")
            );
        } catch (IOException exception) {
            showLogoutWarning(
                    "Não foi possível preparar a tela de login. "
                            + "A sessão atual foi mantida."
            );
            return;
        }
        var session = UserSession.current();
        String token = session == null ? null : session.token();
        logoutInProgress = true;
        // Bloqueia a Home enquanto o logout está em andamento.
        scene.getRoot().setDisable(true);
        authService.logout(token)
                .whenComplete((ignored, failure) ->
                        Platform.runLater(() -> {
                            UserSession.clear();
                            scene.setRoot(loginRoot);
                            logoutInProgress = false;
                            if (failure != null) {
                                showLogoutWarning(
                                        "Você saiu deste aplicativo, mas não foi "
                                                + "possível confirmar a revogação "
                                                + "da sessão no servidor."
                                );
                            }
                        })
                );
    }
    private void showLogoutWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Qualifica");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onPageClick(ActionEvent event) throws IOException {
        Button buttonClicked = (Button) event.getSource();
        String path = (String) buttonClicked.getUserData();

        if (path == null || path.isBlank()) {
            throw new IOException(
                    "Página não configurada para o botão: "
                            + buttonClicked.getText()
            );
        }

        Navigator.changeContent(workspace, path);
    }
}

package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;


import java.io.IOException;

public class HomeController {

    @FXML
    public BorderPane workspace;
    @FXML
    public Label registersCount;

    @FXML
    private void onGoBackClick(ActionEvent event) throws IOException {
        Node botao = (Node) event.getSource();
        Scene scene = botao.getScene();

        Navigator.changeScreen(
                scene,
                "/com/qualificamaisanaliticinterface/hello-view.fxml"
        );
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

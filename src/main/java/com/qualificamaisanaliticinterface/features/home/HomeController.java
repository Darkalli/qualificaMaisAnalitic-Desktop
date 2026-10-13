package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.io.IOException;

public class HomeController {

    @FXML
    private void onGoBackClick(ActionEvent event) throws IOException {
        Node botao = (Node) event.getSource();
        Scene scene = botao.getScene();

        Navigator.mudarTela(
                scene,
                "/com/qualificamaisanaliticinterface/hello-view.fxml"
        );
    }

    @FXML
    private void onVoltarClick(ActionEvent event) throws IOException {
        Node botao = (Node) event.getSource();
        Scene scene = botao.getScene();

        Navigator.mudarTela(
                scene,
                "/com/qualificamaisanaliticinterface/hello-view.fxml"
        );
    }
}

package com.qualificamaisanaliticinterface;

import com.qualificamaisanaliticinterface.navigation.Navigator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.io.IOException;

public class HelloController {

    @FXML
    private void onClick(ActionEvent event) throws IOException {
        Node botao = (Node) event.getSource();
        Scene scene = botao.getScene();

        Navigator.changeScreen(scene, "/views/home.fxml");
    }
}

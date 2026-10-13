package com.qualificamaisanaliticinterface.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;

public final class Navigator {

    private Navigator() {
    }

    public static void changeScreen(Scene scene, String caminho)
            throws IOException {

        URL arquivo = Navigator.class.getResource(caminho);

        if (arquivo == null) {
            throw new IOException("Tela não encontrada: " + caminho);
        }

        FXMLLoader loader = new FXMLLoader(arquivo);
        Parent novaTela = loader.load();

        scene.setRoot(novaTela);
    }
    public static void changeContent(BorderPane pane, String pagePath)
            throws IOException {

        URL path = Navigator.class.getResource(pagePath);

        if (path == null) {
            throw new IOException("Tela não encontrada: " + pagePath);
        }

        FXMLLoader loader = new FXMLLoader(path);
        Parent newPage = loader.load();

        pane.setCenter(newPage);
    }
}

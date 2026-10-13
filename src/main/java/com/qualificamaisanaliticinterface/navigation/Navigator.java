package com.qualificamaisanaliticinterface.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.net.URL;

public final class Navigator {

    private Navigator() {
    }

    public static void mudarTela(Scene scene, String caminho)
            throws IOException {

        URL arquivo = Navigator.class.getResource(caminho);

        if (arquivo == null) {
            throw new IOException("Tela não encontrada: " + caminho);
        }

        FXMLLoader loader = new FXMLLoader(arquivo);
        Parent novaTela = loader.load();

        scene.setRoot(novaTela);
    }
}

package com.qualificamaisanaliticinterface.navigation;

import com.qualificamaisanaliticinterface.features.home.HomeController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import java.util.function.Consumer;

import java.io.IOException;
import java.net.URL;

public final class Navigator {

    private Navigator() {
    }

    public static void changeScreen(Scene scene, String path)
            throws IOException {

        URL arquivo = Navigator.class.getResource(path);

        if (arquivo == null) {
            throw new IOException("Tela não encontrada: " + path);
        }

        FXMLLoader loader = new FXMLLoader(arquivo);
        Parent novaTela = loader.load();

        loader = new FXMLLoader(Navigator.class.getResource("/views/window-bar.fxml"));
        Node header = loader.load();
        BorderPane pane = new BorderPane();
        if(path == "/views/login.fxml"){
            WindowBarController controller = loader.getController();
            controller.setMaximise();
        }
        pane.setTop(header);
        pane.setCenter(novaTela);

        scene.setRoot(pane);
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
    public static void openForm(Scene scene, String pagePath) throws IOException {

        URL path = Navigator.class.getResource(pagePath);

        if (path == null) {
            throw new IOException("Tela não encontrada: " + pagePath);
        }
        FXMLLoader loader = new FXMLLoader(path);
        Dialog<Void> dialog = new Dialog<>();
        DialogPane pane = loader.load();

        loader = new FXMLLoader(Navigator.class.getResource("/views/window-bar.fxml"));
        Node header = loader.load();
        WindowBarController controller = loader.getController();
        controller.setMaximise();
        controller.setMinimise();
        pane.setHeader(header);

        configureFormCorners(dialog, pane);
        dialog.initOwner(scene.getWindow());
        dialog.showAndWait();
    }

    public static void openForm(Scene scene, String pagePath, Consumer<?> configure) throws IOException {

        URL path = Navigator.class.getResource(pagePath);

        if (path == null) {
            throw new IOException("Tela não encontrada: " + pagePath);
        }
        FXMLLoader loader = new FXMLLoader(path);
        Dialog<Void> dialog = new Dialog<>();
        DialogPane pane = loader.load();
        configure.accept(loader.getController());

        loader = new FXMLLoader(Navigator.class.getResource("/views/window-bar.fxml"));
        Node header = loader.load();
        WindowBarController controller = loader.getController();
        controller.setMaximise();
        controller.setMinimise();
        pane.setHeader(header);

        configureFormCorners(dialog, pane);
        dialog.initOwner(scene.getWindow());
        dialog.showAndWait();
    }

    private static void configureFormCorners(Dialog<?> dialog, DialogPane pane) {
        dialog.initStyle(StageStyle.TRANSPARENT);
        pane.getStyleClass().add("rounded-form");
        dialog.setDialogPane(pane);
        pane.getScene().setFill(Color.TRANSPARENT);
    }
}

package com.qualificamaisanaliticinterface;

import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.navigation.WindowResizeSupport;
import com.qualificamaisanaliticinterface.session.SessionService;
import com.qualificamaisanaliticinterface.session.UserSession;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletionException;

public class HelloApplication extends Application {

    private final SessionService sessionService = new SessionService();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/views/startup.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        String[] stylesheets = {
                "/css/application.css",
                "/css/navigation.css",
                "/css/boxes.css",
                "/css/tables.css"
        };


        for (String stylesheet : stylesheets) {
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            HelloApplication.class.getResource(stylesheet),
                            "CSS não encontrado: " + stylesheet
                    ).toExternalForm()
            );
        }
        stage.setTitle("Qualifica+Ijui!");
        String iconUrl = Objects.requireNonNull(HelloApplication.class.getResource(
                "/images/logo-qualifica-atalho.png")).toExternalForm();
        for (int size : new int[]{16, 32, 48, 64, 128, 256}) {
            stage.getIcons().add(new Image(iconUrl, size, size, true, true));
        }
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        configureWindowCorners(stage, scene);
        WindowResizeSupport.install(stage, scene);
        stage.show();

        sessionService.restoreSession()
                .whenComplete((result, erro) ->
                        Platform.runLater(() -> {
                            if (erro != null) {
                                showLogin(scene);
                                return;
                            }

                            if (result.isPresent()) {
                                UserSession.start(result.get());
                                showHome(scene);
                            } else {
                                showLogin(scene);
                            }
                        })
                );
    }

    private void configureWindowCorners(Stage stage, Scene scene) {
        scene.setFill(Color.TRANSPARENT);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(scene.widthProperty());
        clip.heightProperty().bind(scene.heightProperty());
        clip.arcWidthProperty().bind(Bindings.when(
                stage.maximizedProperty().or(stage.fullScreenProperty())
        ).then(0).otherwise(24));
        clip.arcHeightProperty().bind(clip.arcWidthProperty());

        scene.getRoot().setClip(clip);
        scene.rootProperty().addListener((observable, previousRoot, newRoot) -> {
            previousRoot.setClip(null);
            newRoot.setClip(clip);
        });
    }

    private void showHome(Scene scene){
        try {
            Navigator.changeScreen(scene, "/views/home.fxml");
            Stage stage = (Stage) scene.getWindow();
            stage.setWidth(1380);
            stage.setHeight(810);
            stage.centerOnScreen();
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private void showLogin(Scene scene){
        try {
            Navigator.changeScreen(scene, "/views/login.fxml");
            Stage stage = (Stage) scene.getWindow();
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}

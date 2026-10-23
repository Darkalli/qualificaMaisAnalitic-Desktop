package com.qualificamaisanaliticinterface;

import com.qualificamaisanaliticinterface.navigation.Navigator;
import com.qualificamaisanaliticinterface.session.SessionService;
import com.qualificamaisanaliticinterface.session.UserSession;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletionException;

public class HelloApplication extends Application {

    private final SessionService sessionService = new SessionService();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/views/Startup.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1380, 810);
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
        stage.setScene(scene);
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

    private void showHome(Scene scene){
        try {
            Navigator.changeScreen(scene, "/views/home.fxml");
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private void showLogin(Scene scene){
        try {
            Navigator.changeScreen(scene, "/views/login.fxml");
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}

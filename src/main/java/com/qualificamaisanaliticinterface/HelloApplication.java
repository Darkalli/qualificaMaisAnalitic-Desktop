package com.qualificamaisanaliticinterface;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/views/login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 750);
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
    }
}

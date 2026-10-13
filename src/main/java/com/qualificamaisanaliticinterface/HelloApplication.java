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
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 750);
        scene.getStylesheets().add(
                Objects.requireNonNull(HelloApplication.class
                                .getResource("/css/application.css"))
                        .toExternalForm()
        );
        stage.setTitle("Qualifica+Ijui!");
        stage.setScene(scene);
        stage.show();
    }
}

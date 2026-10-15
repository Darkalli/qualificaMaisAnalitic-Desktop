package com.qualificamaisanaliticinterface.features.home;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

public class SupersetController {

    @FXML
    private WebView supersetWebView;

    @FXML
    private void initialize() {
        supersetWebView.getEngine().load("https://youtu.be/dQw4w9WgXcQ");
    }
}

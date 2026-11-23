package com.qualificamaisanaliticinterface.features.home;

import com.qualificamaisanaliticinterface.config.AppConfig;
import com.qualificamaisanaliticinterface.session.SupersetSession;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SupersetController {

    @FXML
    private WebView supersetWebView;

    @FXML
    private void initialize() {
        WebEngine engine = supersetWebView.getEngine();
        SupersetSession.install(engine, AppConfig.supersetBaseUrl());

        String css = """
                *::-webkit-scrollbar {
                    width: 10px !important;
                    height: 10px !important;
                    background: #181a1b !important;
                }

                *::-webkit-scrollbar-track {
                    background: #181a1b !important;
                }

                *::-webkit-scrollbar-thumb {
                    background: #64748b !important;
                    border: 2px solid #181a1b !important;
                    border-radius: 8px !important;
                }

                *::-webkit-scrollbar-thumb:hover {
                    background: #94a3b8 !important;
                }

                *::-webkit-scrollbar-thumb:active {
                    background: #fb923c !important;
                }

                *::-webkit-scrollbar-button {
                    display: none !important;
                    width: 0 !important;
                    height: 0 !important;
                }

                *::-webkit-scrollbar-corner {
                    background: #181a1b !important;
                }
                """;

        String encodedCss = Base64.getEncoder().encodeToString(
                css.getBytes(StandardCharsets.UTF_8)
        );

        engine.setUserStyleSheetLocation(
                "data:text/css;charset=utf-8;base64," + encodedCss
        );

        engine.load(AppConfig.supersetBaseUrl());
    }
}

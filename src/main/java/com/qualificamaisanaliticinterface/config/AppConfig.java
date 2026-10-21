package com.qualificamaisanaliticinterface.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPERTIES = loadProperties();

    private AppConfig(){
    }

    public static String apiBaseUrl() {
        String value = PROPERTIES.getProperty("api.base-url");

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("O Url da api não foi configurada.");
        }

        return value.strip().replaceAll("/+$", "");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input = AppConfig.class.getResourceAsStream("/application.properties")){
            if (input == null) {
                throw new IllegalStateException("Arquivo application.properties não encontrado");
            }

            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)){
                properties.load(reader);
            }
            return properties;
        }catch (IOException exception){
            throw new IllegalStateException("Não foi possível carregar application.properties.", exception);
        }
    }

}
package com.qualificamaisanaliticinterface.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class AppConfig {
    private static final String API_KEY = "api.base-url";
    private static final String SUPERSET_KEY = "superset-url";
    private static final Properties DEFAULTS = loadProperties();
    private static final Path CONNECTIONS_FILE = connectionsFile();
    private static final ConnectionAddresses ACTIVE = configuredAddresses();

    private AppConfig() {
    }

    public static String apiBaseUrl() {
        return ACTIVE.api();
    }

    public static String supersetBaseUrl() {
        return ACTIVE.superset();
    }

    public static boolean usesDefaultApiAddress() {
        return apiBaseUrl().equals(readAddress(DEFAULTS, API_KEY));
    }

    static ConnectionAddresses configuredAddresses() {
        try {
            return addressesFrom(readConnectionProperties());
        } catch (IOException | IllegalArgumentException exception) {
            System.getLogger(AppConfig.class.getName()).log(System.Logger.Level.WARNING,
                    "Não foi possível ler os endereços salvos. Usando os endereços padrão.", exception);
            return addressesFrom(DEFAULTS);
        }
    }

    static ConnectionAddresses resolveConnectionChanges(String apiInput, String supersetInput) {
        ConnectionAddresses current = configuredAddresses();
        return new ConnectionAddresses(
                normalizeInput(apiInput, current.api(), "da API", true),
                normalizeInput(supersetInput, current.superset(), "do Superset", false));
    }

    static synchronized void saveConnections(ConnectionAddresses changes) throws IOException {
        Properties properties = readConnectionProperties();
        ConnectionAddresses current = addressesFrom(properties);
        boolean apiChanged = !changes.api().isEmpty() && !changes.api().equals(current.api());
        boolean supersetChanged = !changes.superset().isEmpty() && !changes.superset().equals(current.superset());
        if (!apiChanged && !supersetChanged) {
            return;
        }

        if (apiChanged) {
            properties.setProperty(API_KEY, changes.api());
        }
        if (supersetChanged) {
            properties.setProperty(SUPERSET_KEY, changes.superset());
        }
        Path directory = Files.createDirectories(CONNECTIONS_FILE.getParent());
        Path temporary = Files.createTempFile(directory, "connections-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, null);
            }
            Files.move(temporary, CONNECTIONS_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String normalizeInput(String input, String current, String field, boolean api) {
        if (input.isBlank()) {
            return "";
        }

        String url = input.strip();
        try {
            boolean hasScheme = url.contains("://");
            URI previous = URI.create(current);
            String prefix = previous.getScheme() + "://";
            URI address = URI.create(hasScheme ? url : prefix + url);
            String scheme = address.getScheme();
            int port = address.getPort();
            if (address.getHost() == null || address.getUserInfo() != null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || port == 0 || port > 65535 || address.getRawAuthority().endsWith(":")
                    || (api && (address.getRawQuery() != null || address.getRawFragment() != null))) {
                throw new IllegalArgumentException();
            }

            if (!hasScheme && port == -1 && previous.getPort() != -1) {
                String authority = address.getRawAuthority();
                url = prefix + authority + ":" + previous.getPort() + url.substring(authority.length());
            } else {
                url = address.toString();
            }
            return stripTrailingPathSlashes(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Informe um endereço válido " + field + " (IP ou URL HTTP/HTTPS).",
                    exception);
        }
    }

    private static Properties readConnectionProperties() throws IOException {
        Properties properties = new Properties(DEFAULTS);
        try (var reader = Files.newBufferedReader(CONNECTIONS_FILE, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (NoSuchFileException ignored) {
            // Sem configuração local, mantenha os endereços do aplicativo.
        }
        return properties;
    }

    private static ConnectionAddresses addressesFrom(Properties properties) {
        return new ConnectionAddresses(readAddress(properties, API_KEY), readAddress(properties, SUPERSET_KEY));
    }

    private static String readAddress(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Endereço não configurado: " + key);
        }
        return stripTrailingPathSlashes(value.strip());
    }

    private static String stripTrailingPathSlashes(String url) {
        return url.contains("?") || url.contains("#") ? url : url.replaceAll("/+$", "");
    }

    private static Path connectionsFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path directory = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".config", "qualifica-mais")
                : Path.of(localAppData, "QualificaMais");
        return directory.resolve("connections.properties");
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

    record ConnectionAddresses(String api, String superset) {
    }
}

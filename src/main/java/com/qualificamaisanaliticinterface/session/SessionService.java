package com.qualificamaisanaliticinterface.session;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.config.AppConfig;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SessionService {

    private final TokenStore store;
    private final Gson gson = new Gson();
    private final HttpClient client;
    private final String baseUrl;
    private final Clock clock;

    public SessionService() {
        this(defaultTokenStore(), HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build(),
                AppConfig.apiBaseUrl(), Clock.systemUTC());
    }

    private static TokenStore defaultTokenStore() {
        if (AppConfig.usesDefaultApiAddress()) {
            return new WindowsTokenStore();
        }

        try {
            byte[] address = AppConfig.apiBaseUrl().getBytes(StandardCharsets.UTF_8);
            String server = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(address));
            return new WindowsTokenStore("session-" + server + ".dat");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Não foi possível identificar o endereço da API.", exception);
        }
    }

    public SessionService(TokenStore store, HttpClient client, String baseUrl, Clock clock) {
        this.store = store;
        this.client = client;
        this.baseUrl = baseUrl;
        this.clock = clock;
    }

    public CompletableFuture<Optional<LoginResponse>> restoreSession() {
        return CompletableFuture.supplyAsync(() -> {
                    Optional<LoginResponse> session;
                    try {
                        session = loadSession();
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                    return session;
                })
                .thenCompose(session -> {
                    if (session.isEmpty()) {
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                    return refreshSession(session.map(LoginResponse::token).orElse(null));
                })
                .thenApply(session ->{
                    if (session.isPresent()) {
                        try {
                            saveSession(session.get());
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    }
                    return session;
                }).exceptionally(failure ->{
                    Throwable cause = failure;
                    while (cause instanceof CompletionException && cause.getCause() != null){
                        cause = cause.getCause();
                    }
                    if(cause instanceof ApiException && ((ApiException) cause).getStatusCode() == 401){
                        try {
                            clearSession();
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        }
                    }else {
                        throw new CompletionException(cause);
                    }
                    return Optional.empty();
                });
    }

    public void saveSession(LoginResponse session) throws IOException {
        if (!responseValidator(session)) {
            throw new IllegalArgumentException("Sessão invalida.");
        }
        store.save(gson.toJson(session));
    }

    private Optional<LoginResponse> loadSession() throws IOException {
        Optional<String> saved = store.load();
        if (saved.isEmpty()) return Optional.empty();
        try {
            LoginResponse session = gson.fromJson(saved.get(), LoginResponse.class);
            if (responseValidator(session)) return Optional.of(session);
        } catch (JsonParseException invalidJson) {
            // A sessão local inválida deve levar ao login, sem tentar renová-la.
        }
        clearSession();
        return Optional.empty();
    }

    private CompletableFuture<Optional<LoginResponse>> refreshSession(String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/refresh"))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status == 200) {
                        LoginResponse Session = gson.fromJson(response.body(), LoginResponse.class);
                        if (!responseValidator(Session)) {
                            throw new IllegalArgumentException("Sessão Invalida.");
                        }
                        return Optional.of(Session);
                    } else if (status == 401) {
                        throw new ApiException(status, "Token invalido");
                    }
                    throw new ApiException(status, "Falha ao renovar sessão");
                });
    }

    public void clearSession() throws IOException {
        store.clear();
    }

    private boolean responseValidator(LoginResponse session) {
        if (session == null || session.token() == null
                || session.token().isBlank()
                || !"Bearer".equals(session.tokenType())
                || session.expiresAt() == null || session.user() == null) {
            return false;
        }
        try {
            return Instant.parse(session.expiresAt()).isAfter(clock.instant());
        } catch (DateTimeParseException invalidDate) {
            return false;
        }
    }
}

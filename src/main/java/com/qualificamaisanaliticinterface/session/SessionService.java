package com.qualificamaisanaliticinterface.session;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.config.AppConfig;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SessionService {

    private final WindowsTokenStore store = new WindowsTokenStore();
    private final Gson gson = new Gson();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

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
        return store.load().map(json -> gson.fromJson(json, LoginResponse.class));
    }

    private CompletableFuture<Optional<LoginResponse>> refreshSession(String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.apiBaseUrl() + "/api/auth/refresh"))
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
        return session != null && session.token() != null
                && !session.token().isBlank()
                && "Bearer".equals(session.tokenType())
                && session.expiresAt() != null
                && !Instant.now().isAfter(Instant.parse(session.expiresAt()))
                && session.user() != null;
    }
}

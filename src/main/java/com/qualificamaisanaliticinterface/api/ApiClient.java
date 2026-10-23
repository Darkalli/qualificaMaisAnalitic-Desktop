package com.qualificamaisanaliticinterface.api;

import com.qualificamaisanaliticinterface.config.AppConfig;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;
import com.qualificamaisanaliticinterface.session.UserSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ApiClient {
    private static final String BASE_URL = AppConfig.apiBaseUrl();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public CompletableFuture<String> get(String path) {
        if (path == null || !path.startsWith("/api/")) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Informe um caminho iniciado por /api/."
                    )
            );
        }
        LoginResponse session = UserSession.current();
        if (session == null
                || session.token() == null
                || session.token().isBlank()) {
            return CompletableFuture.failedFuture(
                    new ApiException(401, "Entre novamente para continuar.")
            );
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + session.token())
                .GET()
                .build();
        return CLIENT.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            }
            String message = switch (status) {
                case 400 -> "Confira os dados enviados na consulta.";
                case 401 -> "Sessão inválida ou expirada. Entre novamente.";
                case 403 -> "Você não tem permissão para esta operação.";
                case 404 -> "Recurso não encontrado.";
                case 409 -> "A operação conflita com os dados existentes.";
                default -> "Não foi possível concluir a consulta.";
            };
            throw new ApiException(status, message);
        });
    }

    public CompletableFuture<String> post(String path, String json) {
        if (path == null || !path.startsWith("/api/")) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Informe um caminho iniciado por /api/."
                    )
            );
        }
        LoginResponse session = UserSession.current();
        if (session == null
                || session.token() == null
                || session.token().isBlank()) {
            return CompletableFuture.failedFuture(
                    new ApiException(401, "Entre novamente para continuar.")
            );
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + session.token())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return CLIENT.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            }
            String message = switch (status) {
                case 400 -> "Confira os dados enviados na postagem.";
                case 401 -> "Sessão inválida ou expirada. Entre novamente.";
                case 403 -> "Você não tem permissão para esta operação.";
                case 404 -> "Recurso não encontrado.";
                case 409 -> "A operação conflita com os dados existentes.";
                default -> "Não foi possível concluir a consulta.";
            };
            throw new ApiException(status, message);
        });
    }
}

package com.qualificamaisanaliticinterface.features.login;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.qualificamaisanaliticinterface.config.AppConfig;
import com.qualificamaisanaliticinterface.features.login.dto.LoginRequest;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class AuthService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Gson gson = new Gson();

    public CompletableFuture<LoginResponse> login(
            String username,
            String password
    ) {
        String json = gson.toJson(new LoginRequest(username, password));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.apiBaseUrl() + "/api/auth/login"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return CLIENT.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            if (response.statusCode() == 401) {
                throw new LoginException("Usuário ou senha inválidos.");
            }

            if (response.statusCode() == 400) {
                throw new LoginException(
                        "Confira o formato do usuário e da senha."
                );
            }

            if (response.statusCode() != 200) {
                throw new LoginException(
                        "Não foi possível entrar. Tente novamente mais tarde."
                );
            }

            try {
                LoginResponse result = gson.fromJson(
                        response.body(),
                        LoginResponse.class
                );

                if (result == null
                        || result.token() == null
                        || result.token().isBlank()
                        || !"Bearer".equals(result.tokenType())
                        || result.user() == null
                        || result.expiresAt() == null) {
                    throw new LoginException(
                            "A API retornou uma resposta de login incompleta."
                    );
                }

                return result;

            } catch (JsonParseException exception) {
                throw new LoginException(
                        "Não foi possível interpretar a resposta da API."
                );
            }
        });
    }

    public CompletableFuture<Void> logout(String token){
        if (token == null || token.isBlank()){
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.apiBaseUrl() + "/api/auth/logout"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response ->{
                    int status = response.statusCode();

                    if (status == 204 || status == 401){
                        return null;
                    }

                    throw new LoginException("Não foi possível confirmar o encerramento "
                            + "da sessão no servidor.");
                });
    }
    public static class LoginException extends RuntimeException {

        public LoginException(String message) {
            super(message);
        }
    }
}
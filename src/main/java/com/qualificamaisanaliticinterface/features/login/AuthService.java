package com.qualificamaisanaliticinterface.features.login;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
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
                .uri(URI.create("https://192.168.1.83/api/auth/login"))
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

    public static class LoginException extends RuntimeException {

        public LoginException(String message) {
            super(message);
        }
    }
}
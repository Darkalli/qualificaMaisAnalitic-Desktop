package com.qualificamaisanaliticinterface.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiConnectionCheck {

    public static void main(String[] args) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://192.168.1.83/api/course"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            System.out.println("HTTP: " + response.statusCode());

            if (response.statusCode() == 401) {
                System.out.println(
                        "Conexão estabelecida. A API exige autenticação."
                );
            } else {
                System.out.println(
                        "Resposta diferente da esperada. Confira antes de continuar."
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Verificação interrompida.");

        } catch (Exception exception) {
            System.err.println(
                    "Falha: " + exception.getClass().getSimpleName()
            );
            System.err.println(exception.getMessage());
        }
    }
}
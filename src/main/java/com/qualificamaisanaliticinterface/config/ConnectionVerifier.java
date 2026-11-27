package com.qualificamaisanaliticinterface.config;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class ConnectionVerifier {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final Supplier<HttpClient> client;

    ConnectionVerifier(Supplier<HttpClient> client) {
        this.client = client;
    }

    ConnectionVerifier() {
        this(() -> DefaultClient.INSTANCE);
    }

    CompletableFuture<List<String>> verify(AppConfig.ConnectionAddresses changes) {
        CompletableFuture<String> api = verifyAddress(changes.api(), "API", true);
        CompletableFuture<String> superset = verifyAddress(changes.superset(), "Superset", false);
        return api.thenCombine(superset, (apiError, supersetError) ->
                Stream.of(apiError, supersetError).filter(Objects::nonNull).toList());
    }

    private CompletableFuture<String> verifyAddress(String address, String label, boolean api) {
        if (address.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        URI uri = URI.create(api ? address + "/api/auth/me" : address);
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT)
                .header("Accept", api ? "application/json" : "text/html")
                .GET().build();
        return CompletableFuture.supplyAsync(client)
                .thenCompose(http -> http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()))
                .handle((response, failure) -> {
                    if (failure != null) {
                        return connectionError(label, failure);
                    }
                    try (var body = response.body()) {
                        if (isExpectedResponse(response, api)) {
                            return null;
                        }
                        if (response.statusCode() == 200) {
                            return label + ": o endereço respondeu em um formato inesperado. Confira a URL.";
                        }
                        return label + ": resposta HTTP " + response.statusCode() + ". Confira o endereço informado.";
                    } catch (IOException exception) {
                        return connectionError(label, exception);
                    }
                });
    }

    private boolean isExpectedResponse(HttpResponse<?> response, boolean api) {
        int status = response.statusCode();
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        if (api) {
            return status == 401 || status == 403
                    || (status == 200 && contentType.startsWith("application/json"));
        }
        return (status == 200 && (contentType.startsWith("text/html")
                || contentType.startsWith("application/xhtml+xml"))) || ((status == 301 || status == 302 || status == 303
                || status == 307 || status == 308) && response.headers().firstValue("Location").isPresent());
    }

    private String connectionError(String label, Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause() : failure;
        if (cause instanceof HttpTimeoutException) {
            return label + ": o servidor demorou para responder. Confira o endereço e a conexão.";
        }
        if (cause instanceof SSLException) {
            return label + ": falha no certificado HTTPS. Confira o endereço informado.";
        }
        return label + ": não foi possível conectar. Confira o endereço, a porta e a rede.";
    }

    private static final class DefaultClient {
        private static final HttpClient INSTANCE = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}

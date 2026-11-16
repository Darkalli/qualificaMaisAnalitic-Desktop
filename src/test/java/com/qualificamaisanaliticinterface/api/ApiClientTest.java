package com.qualificamaisanaliticinterface.api;

import com.qualificamaisanaliticinterface.AuthTestSupport.StubHttpClient;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;
import com.qualificamaisanaliticinterface.session.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.qualificamaisanaliticinterface.AuthTestSupport.*;

class ApiClientTest {
    private static final String JSON = "{\"name\":\"João\"}";
    private static final String PATH = "/api/people?name=Ana%20Silva";

    @AfterEach
    void clearSession() {
        UserSession.clear();
    }

    @TestFactory
    Stream<DynamicTest> shouldSendAuthenticatedRequestsAndReturnSuccessfulBody() {
        return Arrays.stream(Operation.values()).flatMap(operation -> Stream.of(200, 201, 204).map(status ->
                DynamicTest.dynamicTest(operation + " HTTP " + status, () -> {
                    UserSession.start(session("api-token"));
                    var http = new StubHttpClient();
                    String body = status == 204 ? "" : "{\"result\":\"ok\"}";
                    http.respond(status, body);
                    Assertions.assertEquals(body, operation.call(new ApiClient(http, BASE_URL), PATH).get(5, TimeUnit.SECONDS));
                    Assertions.assertEquals(1, http.requests.size());
                    var request = http.lastRequest();
                    Assertions.assertEquals(BASE_URL + PATH, request.uri().toString());
                    Assertions.assertEquals(operation.method(), request.method());
                    Assertions.assertEquals("Bearer api-token", request.headers().firstValue("Authorization").orElseThrow());
                    Assertions.assertEquals(Duration.ofSeconds(20), request.timeout().orElseThrow());
                    if (operation != Operation.DELETE) {
                        Assertions.assertEquals("application/json", request.headers().firstValue("Accept").orElseThrow());
                    }
                    if (operation.hasBody()) {
                        Assertions.assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
                        Assertions.assertEquals(JSON, http.requestBody());
                    } else {
                        Assertions.assertTrue(request.bodyPublisher().isEmpty());
                    }
                })));
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidPathsWithoutSendingHttp() {
        return Arrays.stream(Operation.values()).flatMap(operation -> Stream.of(null, "", "api/people", "/people",
                "https://other.test/api/people").map(path -> DynamicTest.dynamicTest(operation + " invalid path: " + path, () -> {
                    UserSession.start(session("token"));
                    var http = new StubHttpClient();
                    Assertions.assertInstanceOf(IllegalArgumentException.class, failure(operation.call(new ApiClient(http, BASE_URL), path)));
                    Assertions.assertTrue(http.requests.isEmpty());
                })));
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectMissingOrBlankSessionWithoutSendingHttp() {
        var valid = session("token");
        return Arrays.stream(Operation.values()).flatMap(operation -> Arrays.asList(null,
                new LoginResponse(null, "Bearer", valid.expiresAt(), valid.user()),
                new LoginResponse("  ", "Bearer", valid.expiresAt(), valid.user())).stream()
                .map(response -> DynamicTest.dynamicTest(operation + " invalid session: " + response, () -> {
                    UserSession.clear();
                    if (response != null) UserSession.start(response);
                    var http = new StubHttpClient();
                    var error = Assertions.assertInstanceOf(ApiException.class,
                            failure(operation.call(new ApiClient(http, BASE_URL), PATH)));
                    Assertions.assertEquals(401, error.getStatusCode());
                    Assertions.assertFalse(error.getMessage().isBlank());
                    Assertions.assertTrue(http.requests.isEmpty());
                })));
    }

    @TestFactory
    Stream<DynamicTest> shouldPreserveHttpStatusAndUseFallbackForNonJsonErrors() {
        return Arrays.stream(Operation.values()).flatMap(operation -> Stream.of(400, 401, 403, 404, 409, 500, 503)
                .map(status -> DynamicTest.dynamicTest(operation + " error HTTP " + status, () -> {
                    UserSession.start(session("token"));
                    var http = new StubHttpClient();
                    http.respond(status, "<html>proxy error</html>");
                    var error = Assertions.assertInstanceOf(ApiException.class,
                            failure(operation.call(new ApiClient(http, BASE_URL), PATH)));
                    Assertions.assertEquals(status.intValue(), error.getStatusCode());
                    Assertions.assertNotNull(error.getMessage());
                    Assertions.assertFalse(error.getMessage().isBlank());
                    Assertions.assertFalse(error.getMessage().contains("<html>"));
                })));
    }

    @TestFactory
    Stream<DynamicTest> shouldExposeMessageReturnedByApi() {
        return Arrays.stream(Operation.values()).map(operation -> DynamicTest.dynamicTest(operation + " API message", () -> {
            UserSession.start(session("token"));
            var http = new StubHttpClient();
            http.respond(409, "{\"message\":\"Registro já existe.\"}");
            var error = Assertions.assertInstanceOf(ApiException.class,
                    failure(operation.call(new ApiClient(http, BASE_URL), PATH)));
            Assertions.assertEquals(409, error.getStatusCode());
            Assertions.assertEquals("Registro já existe.", error.getMessage());
        }));
    }

    @TestFactory
    Stream<DynamicTest> shouldKeepFallbackWhenErrorMessageIsMissing() {
        return Arrays.stream(Operation.values()).flatMap(operation -> Stream.of("", "null", "{}", "{\"message\":null}",
                "{\"message\":\"  \"}", "[]").map(body -> DynamicTest.dynamicTest(operation + " empty API message: " + body, () -> {
                    UserSession.start(session("token"));
                    var http = new StubHttpClient();
                    http.respond(400, body);
                    var error = Assertions.assertInstanceOf(ApiException.class,
                            failure(operation.call(new ApiClient(http, BASE_URL), PATH)));
                    Assertions.assertEquals(400, error.getStatusCode());
                    Assertions.assertNotNull(error.getMessage());
                    Assertions.assertFalse(error.getMessage().isBlank());
                })));
    }

    @TestFactory
    Stream<DynamicTest> shouldPropagateConnectionAndTimeoutFailures() {
        return Arrays.stream(Operation.values()).flatMap(operation -> Stream.of(new IOException("offline"),
                new HttpTimeoutException("timeout")).map(error -> DynamicTest.dynamicTest(operation + " " + error.getMessage(), () -> {
                    UserSession.start(session("token"));
                    var http = new StubHttpClient();
                    http.fail(error);
                    Assertions.assertSame(error, failure(operation.call(new ApiClient(http, BASE_URL), PATH)));
                })));
    }

    @TestFactory
    Stream<DynamicTest> shouldUseCurrentTokenForEachRequest() {
        return Arrays.stream(Operation.values()).map(operation -> DynamicTest.dynamicTest(operation + " session replaced", () -> {
            var http = new StubHttpClient();
            var client = new ApiClient(http, BASE_URL);
            UserSession.start(session("first-token"));
            operation.call(client, PATH).get(5, TimeUnit.SECONDS);
            UserSession.start(session("second-token"));
            operation.call(client, PATH).get(5, TimeUnit.SECONDS);
            Assertions.assertEquals("Bearer first-token", http.requests.getFirst().headers().firstValue("Authorization").orElseThrow());
            Assertions.assertEquals("Bearer second-token", http.lastRequest().headers().firstValue("Authorization").orElseThrow());
        }));
    }

    private enum Operation {
        GET, POST, PATCH, DELETE_WITH_BODY, DELETE;

        CompletableFuture<?> call(ApiClient client, String path) {
            return switch (this) {
                case GET -> client.get(path);
                case POST -> client.post(path, JSON);
                case PATCH -> client.patch(path, JSON);
                case DELETE_WITH_BODY -> client.delete(path, JSON);
                case DELETE -> client.delete(path);
            };
        }

        String method() {
            return this == DELETE_WITH_BODY ? "DELETE" : name();
        }

        boolean hasBody() {
            return this == POST || this == PATCH || this == DELETE_WITH_BODY;
        }
    }
}

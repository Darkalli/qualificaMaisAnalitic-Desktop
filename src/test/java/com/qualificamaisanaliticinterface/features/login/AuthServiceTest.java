package com.qualificamaisanaliticinterface.features.login;

import com.google.gson.JsonParser;
import com.qualificamaisanaliticinterface.AuthTestSupport.StubHttpClient;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.qualificamaisanaliticinterface.AuthTestSupport.*;

class AuthServiceTest {
    private StubHttpClient http;
    private AuthService service;

    @BeforeEach
    void setUp() {
        http = new StubHttpClient();
        service = new AuthService(http, BASE_URL);
    }

    @Test
    void shouldPostCredentialsAndDeserializeLoginResponse() throws Exception {
        var expected = session("login-token");
        http.respond(200, GSON.toJson(expected));
        Assertions.assertEquals(expected, service.login("ana", "senha\"com\\escape").get(5, TimeUnit.SECONDS));
        Assertions.assertEquals(1, http.requests.size());
        var request = http.lastRequest();
        Assertions.assertEquals(BASE_URL + "/api/auth/login", request.uri().toString());
        Assertions.assertEquals("POST", request.method());
        Assertions.assertEquals(Duration.ofSeconds(20), request.timeout().orElseThrow());
        Assertions.assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
        Assertions.assertEquals("application/json", request.headers().firstValue("Accept").orElseThrow());
        Assertions.assertTrue(request.headers().firstValue("Authorization").isEmpty());
        var credentials = JsonParser.parseString(http.requestBody()).getAsJsonObject();
        Assertions.assertEquals("ana", credentials.get("username").getAsString());
        Assertions.assertEquals("senha\"com\\escape", credentials.get("password").getAsString());
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectUnsuccessfulLoginResponses() {
        return Stream.of(400, 401, 403, 429, 500, 503).map(status ->
                DynamicTest.dynamicTest("login HTTP " + status, () -> {
                    setUp();
                    http.respond(status, "<html>erro</html>");
                    var error = Assertions.assertInstanceOf(AuthService.LoginException.class, failure(service.login("ana", "senha")));
                    Assertions.assertNotNull(error.getMessage());
                    Assertions.assertFalse(error.getMessage().isBlank());
                    Assertions.assertFalse(error.getMessage().contains("<html>"));
                }));
    }

    @Test
    void shouldDistinguishInvalidCredentialsFromServerFailure() {
        http.respond(401, "{}");
        var unauthorized = failure(service.login("ana", "wrong"));
        http.respond(500, "{}");
        var serverFailure = failure(service.login("ana", "password"));
        Assertions.assertNotEquals(unauthorized.getMessage(), serverFailure.getMessage());
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectIncompleteLoginResponses() {
        var valid = session("token");
        return Stream.of("null", "{}",
                GSON.toJson(new LoginResponse(null, "Bearer", valid.expiresAt(), valid.user())),
                GSON.toJson(new LoginResponse("  ", "Bearer", valid.expiresAt(), valid.user())),
                GSON.toJson(new LoginResponse("token", "Basic", valid.expiresAt(), valid.user())),
                GSON.toJson(new LoginResponse("token", "Bearer", null, valid.user())),
                GSON.toJson(new LoginResponse("token", "Bearer", valid.expiresAt(), null)))
                .map(body -> DynamicTest.dynamicTest("incomplete login: " + body, () -> {
                    setUp();
                    http.respond(200, body);
                    Assertions.assertInstanceOf(AuthService.LoginException.class, failure(service.login("ana", "senha")));
                }));
    }

    @TestFactory
    Stream<DynamicTest> shouldReportMalformedLoginJson() {
        return Stream.of("<html>error</html>", "{", "[]").map(body ->
                DynamicTest.dynamicTest("malformed JSON: " + body, () -> {
                    setUp();
                    http.respond(200, body);
                    Assertions.assertInstanceOf(AuthService.LoginException.class, failure(service.login("ana", "senha")));
                }));
    }

    @Test
    void shouldPropagateConnectionFailureDuringLogin() {
        IOException offline = new IOException("offline");
        http.fail(offline);
        Assertions.assertSame(offline, failure(service.login("ana", "senha")));
    }

    @TestFactory
    Stream<DynamicTest> shouldSkipLogoutWithoutToken() {
        return Stream.of(null, "", "  ").map(token -> DynamicTest.dynamicTest("logout without token: " + token, () -> {
            setUp();
            Assertions.assertNull(service.logout(token).get(5, TimeUnit.SECONDS));
            Assertions.assertTrue(http.requests.isEmpty());
        }));
    }

    @TestFactory
    Stream<DynamicTest> shouldAcceptRevokedOrAlreadyInvalidToken() {
        return Stream.of(204, 401).map(status -> DynamicTest.dynamicTest("logout HTTP " + status, () -> {
            setUp();
            http.respond(status, "");
            Assertions.assertNull(service.logout("logout-token").get(5, TimeUnit.SECONDS));
            Assertions.assertEquals(1, http.requests.size());
            var request = http.lastRequest();
            Assertions.assertEquals(BASE_URL + "/api/auth/logout", request.uri().toString());
            Assertions.assertEquals("POST", request.method());
            Assertions.assertEquals("Bearer logout-token", request.headers().firstValue("Authorization").orElseThrow());
            Assertions.assertEquals(Duration.ofSeconds(20), request.timeout().orElseThrow());
            Assertions.assertEquals("", http.requestBody());
        }));
    }

    @TestFactory
    Stream<DynamicTest> shouldReportLogoutServerFailure() {
        return Stream.of(400, 403, 500, 503).map(status -> DynamicTest.dynamicTest("logout failure HTTP " + status, () -> {
            setUp();
            http.respond(status, "{}");
            Assertions.assertInstanceOf(AuthService.LoginException.class, failure(service.logout("token")));
        }));
    }

    @Test
    void shouldPropagateConnectionFailureDuringLogout() {
        IOException offline = new IOException("offline");
        http.fail(offline);
        Assertions.assertSame(offline, failure(service.logout("token")));
    }
}

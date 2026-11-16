package com.qualificamaisanaliticinterface.session;

import com.qualificamaisanaliticinterface.AuthTestSupport.MemoryTokenStore;
import com.qualificamaisanaliticinterface.AuthTestSupport.StubHttpClient;
import com.qualificamaisanaliticinterface.api.ApiException;
import com.qualificamaisanaliticinterface.features.login.dto.LoginResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.qualificamaisanaliticinterface.AuthTestSupport.*;

class SessionServiceTest {
    private MemoryTokenStore store;
    private StubHttpClient http;
    private SessionService service;

    @BeforeEach
    void setUp() {
        store = new MemoryTokenStore();
        http = new StubHttpClient();
        service = new SessionService(store, http, BASE_URL, CLOCK);
    }

    @Test
    void shouldSaveCompleteSessionAsJson() throws Exception {
        var response = session("local-token");
        service.saveSession(response);
        Assertions.assertEquals(response, GSON.fromJson(store.saved, LoginResponse.class));
        Assertions.assertEquals(1, store.saves);
        Assertions.assertTrue(http.requests.isEmpty());
    }

    @TestFactory
    Stream<DynamicTest> shouldRejectInvalidSessionBeforeSaving() {
        return java.util.stream.IntStream.range(0, invalidSessions().size()).mapToObj(index ->
                DynamicTest.dynamicTest("invalid session " + index, () -> {
                    setUp();
                    Assertions.assertThrows(IllegalArgumentException.class, () -> service.saveSession(invalidSessions().get(index)));
                    Assertions.assertEquals(0, store.saves);
                    Assertions.assertNull(store.saved);
                }));
    }

    @Test
    void shouldPropagateSaveFailure() {
        store.saveFailure = new IOException("disk unavailable");
        Assertions.assertSame(store.saveFailure, Assertions.assertThrows(IOException.class, () -> service.saveSession(session("token"))));
    }

    @Test
    void shouldSkipRefreshWhenNoSessionWasSaved() throws Exception {
        Assertions.assertTrue(service.restoreSession().get(5, TimeUnit.SECONDS).isEmpty());
        Assertions.assertEquals(1, store.loads);
        Assertions.assertEquals(0, store.saves);
        Assertions.assertEquals(0, store.clears);
        Assertions.assertTrue(http.requests.isEmpty());
    }

    @TestFactory
    Stream<DynamicTest> shouldClearCorruptOrExpiredLocalSessionsWithoutHttp() {
        var invalidJson = new java.util.ArrayList<>(List.of("not-json", "null", "{}", "[]"));
        invalidSessions().stream().map(GSON::toJson).forEach(invalidJson::add);
        return java.util.stream.IntStream.range(0, invalidJson.size()).mapToObj(index ->
                DynamicTest.dynamicTest("invalid saved session " + index, () -> {
                    setUp();
                    store.saved = invalidJson.get(index);
                    Assertions.assertTrue(service.restoreSession().get(5, TimeUnit.SECONDS).isEmpty());
                    Assertions.assertNull(store.saved);
                    Assertions.assertEquals(1, store.clears);
                    Assertions.assertEquals(0, store.saves);
                    Assertions.assertTrue(http.requests.isEmpty());
                }));
    }

    @Test
    void shouldRefreshAndPersistNewSession() throws Exception {
        store.saved = GSON.toJson(session("old-token"));
        var refreshed = session("new-token");
        http.respond(200, GSON.toJson(refreshed));

        Assertions.assertEquals(refreshed, service.restoreSession().get(5, TimeUnit.SECONDS).orElseThrow());
        Assertions.assertEquals(refreshed, GSON.fromJson(store.saved, LoginResponse.class));
        Assertions.assertEquals(1, store.saves);
        Assertions.assertEquals(0, store.clears);
        Assertions.assertEquals(1, http.requests.size());
        var request = http.lastRequest();
        Assertions.assertEquals(BASE_URL + "/api/auth/refresh", request.uri().toString());
        Assertions.assertEquals("POST", request.method());
        Assertions.assertEquals("Bearer old-token", request.headers().firstValue("Authorization").orElseThrow());
        Assertions.assertEquals("application/json", request.headers().firstValue("Accept").orElseThrow());
        Assertions.assertEquals(Duration.ofSeconds(5), request.timeout().orElseThrow());
        Assertions.assertEquals("", http.requestBody());
    }

    @Test
    void shouldClearSessionWhenRefreshIsUnauthorized() throws Exception {
        store.saved = GSON.toJson(session("revoked-token"));
        http.respond(401, "{}");
        Assertions.assertTrue(service.restoreSession().get(5, TimeUnit.SECONDS).isEmpty());
        Assertions.assertNull(store.saved);
        Assertions.assertEquals(1, store.clears);
        Assertions.assertEquals(0, store.saves);
    }

    @TestFactory
    Stream<DynamicTest> shouldKeepSavedSessionOnTemporaryServerFailure() {
        return Stream.of(403, 429, 500, 503).map(status -> DynamicTest.dynamicTest("refresh HTTP " + status, () -> {
            setUp();
            String saved = GSON.toJson(session("old-token"));
            store.saved = saved;
            http.respond(status, "{}");
            var error = Assertions.assertInstanceOf(ApiException.class, failure(service.restoreSession()));
            Assertions.assertEquals(status.intValue(), error.getStatusCode());
            Assertions.assertEquals(saved, store.saved);
            Assertions.assertEquals(0, store.clears);
            Assertions.assertEquals(0, store.saves);
        }));
    }

    @TestFactory
    Stream<DynamicTest> shouldNotOverwriteSessionWithInvalidRefreshResponse() {
        return Stream.of("not-json", "null", "{}", GSON.toJson(new LoginResponse("", "Bearer",
                session("token").expiresAt(), session("token").user())),
                GSON.toJson(new LoginResponse("token", "Bearer", "2026-11-15T00:00:00Z", session("token").user())))
                .map(body -> DynamicTest.dynamicTest("invalid refresh: " + body, () -> {
                    setUp();
                    String saved = GSON.toJson(session("old-token"));
                    store.saved = saved;
                    http.respond(200, body);
                    Assertions.assertNotNull(failure(service.restoreSession()));
                    Assertions.assertEquals(saved, store.saved);
                    Assertions.assertEquals(0, store.saves);
                    Assertions.assertEquals(0, store.clears);
                }));
    }

    @Test
    void shouldKeepSavedSessionOnConnectionFailure() {
        String saved = GSON.toJson(session("old-token"));
        store.saved = saved;
        IOException offline = new IOException("offline");
        http.fail(offline);
        Assertions.assertSame(offline, failure(service.restoreSession()));
        Assertions.assertEquals(saved, store.saved);
        Assertions.assertEquals(0, store.clears);
    }

    @Test
    void shouldPropagateLoadFailureWithoutMakingHttpRequest() {
        store.loadFailure = new IOException("cannot read");
        Assertions.assertSame(store.loadFailure, failure(service.restoreSession()));
        Assertions.assertTrue(http.requests.isEmpty());
        Assertions.assertEquals(0, store.clears);
    }

    @Test
    void shouldPropagateFailureToSaveRefreshedSession() {
        store.saved = GSON.toJson(session("old-token"));
        store.saveFailure = new IOException("cannot save");
        http.respond(200, GSON.toJson(session("new-token")));
        Assertions.assertSame(store.saveFailure, failure(service.restoreSession()));
        Assertions.assertEquals("old-token", GSON.fromJson(store.saved, LoginResponse.class).token());
    }

    @Test
    void shouldPropagateFailureToClearRejectedSession() {
        store.saved = GSON.toJson(session("old-token"));
        store.clearFailure = new IOException("cannot delete");
        http.respond(401, "{}");
        Assertions.assertSame(store.clearFailure, failure(service.restoreSession()));
        Assertions.assertNotNull(store.saved);
    }

    @Test
    void shouldPropagateFailureToClearCorruptSessionWithoutHttp() {
        store.saved = "corrupt";
        store.clearFailure = new IOException("cannot delete");
        Assertions.assertSame(store.clearFailure, failure(service.restoreSession()));
        Assertions.assertTrue(http.requests.isEmpty());
    }

    @Test
    void shouldClearSavedSession() throws Exception {
        store.saved = GSON.toJson(session("token"));
        service.clearSession();
        Assertions.assertNull(store.saved);
        Assertions.assertEquals(1, store.clears);
    }

    private List<LoginResponse> invalidSessions() {
        var valid = session("token");
        return Arrays.asList(null,
                new LoginResponse(null, "Bearer", valid.expiresAt(), valid.user()),
                new LoginResponse("  ", "Bearer", valid.expiresAt(), valid.user()),
                new LoginResponse("token", "Basic", valid.expiresAt(), valid.user()),
                new LoginResponse("token", null, valid.expiresAt(), valid.user()),
                new LoginResponse("token", "Bearer", null, valid.user()),
                new LoginResponse("token", "Bearer", "invalid-date", valid.user()),
                new LoginResponse("token", "Bearer", CLOCK.instant().toString(), valid.user()),
                new LoginResponse("token", "Bearer", "2026-11-15T12:00:00Z", valid.user()),
                new LoginResponse("token", "Bearer", valid.expiresAt(), null));
    }
}

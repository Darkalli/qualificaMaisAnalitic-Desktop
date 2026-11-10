package com.qualificamaisanaliticinterface.features.registers;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.qualificamaisanaliticinterface.features.registers.RegisterTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class RegisterServiceTest {
    @Test
    void findAllParsesRegistrations() {
        FakeApiClient api = new FakeApiClient();
        api.response = "[" + new Gson().toJson(register(StatusRegister.ACTIVE)) + "]";

        var results = new RegisterService(api).findAll().join();

        assertEquals("/api/register", api.path);
        assertEquals(1, results.size());
        assertEquals(11L, results.getFirst().id());
        assertEquals("Ana Silva", results.getFirst().person().fullName());
        assertEquals("Curso A", results.getFirst().courseOfInterest().name());
        assertEquals(StatusRegister.ACTIVE, results.getFirst().status());
    }

    @Test
    void findAllRejectsNullResponse() {
        FakeApiClient api = new FakeApiClient();
        api.response = "null";

        CompletionException error = assertThrows(CompletionException.class, () -> new RegisterService(api).findAll().join());
        assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals("A API retornou uma lista de inscrições inválida.", error.getCause().getMessage());
    }

    @Test
    void findCoursesParsesAvailableCourses() {
        FakeApiClient api = new FakeApiClient();
        api.response = "[" + new Gson().toJson(course()) + "]";

        var results = new RegisterService(api).findCourses().join();

        assertEquals("/api/course", api.path);
        assertEquals(1, results.size());
        assertEquals(course(), results.getFirst());
    }

    @Test
    void addRegisterSendsCpfCourseAndDate() {
        FakeApiClient api = new FakeApiClient();
        new RegisterService(api).addRegister("12345678901", 3L, "2026-11-10").join();

        assertEquals("POST", api.method);
        assertEquals("/api/register", api.path);
        var body = JsonParser.parseString(api.body).getAsJsonObject();
        assertEquals("12345678901", body.get("personCpf").getAsString());
        assertEquals(3L, body.get("courseOfInterestId").getAsLong());
        assertEquals("2026-11-10", body.get("registerDate").getAsString());
    }

    @Test
    void cancelRegisterSendsLookupFields() {
        FakeApiClient api = new FakeApiClient();
        new RegisterService(api).cancelRegister(register(StatusRegister.ACTIVE)).join();

        assertEquals("DELETE", api.method);
        assertEquals("/api/register", api.path);
        assertLookupBody(api.body);
    }

    @Test
    void reactivateRegisterSendsLookupFields() {
        FakeApiClient api = new FakeApiClient();
        new RegisterService(api).reactiveRegister(register(StatusRegister.CANCELED)).join();

        assertEquals("PATCH", api.method);
        assertEquals("/api/register/active", api.path);
        assertLookupBody(api.body);
    }

    private static void assertLookupBody(String json) {
        var body = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("12345678901", body.get("personCpf").getAsString());
        assertEquals(3L, body.get("courseOfInterestId").getAsLong());
    }

    private static class FakeApiClient extends ApiClient {
        String method;
        String path;
        String body;
        String response = "ok";

        @Override public CompletableFuture<String> get(String path) {
            method = "GET"; this.path = path; return CompletableFuture.completedFuture(response);
        }
        @Override public CompletableFuture<String> post(String path, String body) {
            method = "POST"; this.path = path; this.body = body; return CompletableFuture.completedFuture(response);
        }
        @Override public CompletableFuture<String> patch(String path, String body) {
            method = "PATCH"; this.path = path; this.body = body; return CompletableFuture.completedFuture(response);
        }
        @Override public CompletableFuture<String> delete(String path, String body) {
            method = "DELETE"; this.path = path; this.body = body; return CompletableFuture.completedFuture(response);
        }
    }
}

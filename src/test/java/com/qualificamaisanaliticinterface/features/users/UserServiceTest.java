package com.qualificamaisanaliticinterface.features.users;

import com.google.gson.JsonParser;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.enuns.UserRole;
import com.qualificamaisanaliticinterface.features.users.dto.UserDto;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    @Test
    void findAllParsesUsers() {
        FakeApiClient api = new FakeApiClient();
        api.response = "[{\"id\":7,\"username\":\"ana\",\"role\":\"ADMIN\"}]";

        var users = new UserService(api).findAll().join();

        assertEquals("/api/auth/all", api.path);
        assertEquals(new UserDto(7L, "ana", UserRole.ADMIN), users.getFirst());
    }

    @Test
    void findAllRejectsNullResponse() {
        FakeApiClient api = new FakeApiClient();
        api.response = "null";

        CompletionException failure = assertThrows(CompletionException.class, () -> new UserService(api).findAll().join());
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals("A API retornou uma lista de usuários inválida.", failure.getCause().getMessage());
    }

    @Test
    void addUserSendsUsernamePasswordAndRole() {
        FakeApiClient api = new FakeApiClient();
        new UserService(api).addUser("ana", "senha1234", UserRole.ADMIN).toCompletableFuture().join();

        assertEquals("POST", api.method);
        assertEquals("/api/auth/register", api.path);
        var body = JsonParser.parseString(api.body).getAsJsonObject();
        assertEquals("ana", body.get("username").getAsString());
        assertEquals("senha1234", body.get("password").getAsString());
        assertEquals("ADMIN", body.get("role").getAsString());
    }

    @Test
    void updateUserSendsIdUsernameAndRole() {
        FakeApiClient api = new FakeApiClient();
        new UserService(api).updateUser(7L, "ana2", UserRole.AGENT).join();

        assertEquals("PATCH", api.method);
        assertEquals("/api/auth", api.path);
        var body = JsonParser.parseString(api.body).getAsJsonObject();
        assertEquals(7L, body.get("id").getAsLong());
        assertEquals("ana2", body.get("username").getAsString());
        assertEquals("AGENT", body.get("role").getAsString());
    }

    @Test
    void deleteUserSendsId() {
        FakeApiClient api = new FakeApiClient();
        new UserService(api).deleteUser(7L).join();

        assertEquals("DELETE", api.method);
        assertEquals("/api/auth/7", api.path);
        assertEquals("7", api.body);
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

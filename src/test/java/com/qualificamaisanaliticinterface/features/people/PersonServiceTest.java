package com.qualificamaisanaliticinterface.features.people;

import com.qualificamaisanaliticinterface.api.ApiClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class PersonServiceTest {
    @Test
    void findAllReadsPeopleFromApi() {
        FakeApiClient api = new FakeApiClient("[{\"id\":7,\"fullName\":\"Ana Silva\",\"cpf\":\"12345678901\"}]");

        var people = new PersonService(api).findAll().join();

        assertEquals("/api/person", api.path);
        assertEquals(1, people.size());
        assertEquals(7L, people.getFirst().id());
        assertEquals("Ana Silva", people.getFirst().fullName());
        assertEquals("12345678901", people.getFirst().cpf());
    }

    @Test
    void findAllAcceptsEmptyList() {
        assertTrue(new PersonService(new FakeApiClient("[]")).findAll().join().isEmpty());
    }

    @Test
    void findAllRejectsNullList() {
        CompletionException failure = assertThrows(CompletionException.class,
                () -> new PersonService(new FakeApiClient("null")).findAll().join());
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals("A API retornou uma lista de pessoas inválida.", failure.getCause().getMessage());
    }

    private static final class FakeApiClient extends ApiClient {
        private final String response;
        private String path;

        private FakeApiClient(String response) {
            this.response = response;
        }

        @Override
        public CompletableFuture<String> get(String path) {
            this.path = path;
            return CompletableFuture.completedFuture(response);
        }
    }
}

package com.qualificamaisanaliticinterface.features.classes;

import com.google.gson.JsonParser;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.classes.dto.ClassDto;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class ClassServiceTest {
    @Test
    void shouldParseClassesAndPresencesFromApiClassService() {
        RecordingApiClient api = new RecordingApiClient();
        api.responses = Map.of(
                "/api/courseClass", "[{\"id\":7,\"day\":\"2026-11-20\",\"session\":\"NIGHT\",\"start\":\"19:00\",\"finish\":\"21:00\",\"statusClass\":\"ACTIVE\",\"course\":{\"id\":3,\"name\":\"Curso A\"}}]",
                "/api/presence?courseId=3&courseClassId=7", "[{\"Id\":9,\"person\":{\"id\":4,\"fullName\":\"Ana\"},\"status\":\"PRESENT\"}]"
        );
        ClassService service = new ClassService(api);

        var classes = service.findAll().join();
        var presences = service.findAllPresences(3L, 7L).join();

        assertEquals(List.of(new Request("GET", "/api/courseClass", null),
                new Request("GET", "/api/presence?courseId=3&courseClassId=7", null)), api.requests);
        assertEquals(1, classes.size());
        assertEquals(7L, classes.getFirst().id());
        assertEquals("2026-11-20", classes.getFirst().day());
        assertEquals(Period.NIGHT, classes.getFirst().session());
        assertEquals(StatusClass.ACTIVE, classes.getFirst().statusClass());
        assertEquals(3L, classes.getFirst().course().id());
        assertEquals("Curso A", classes.getFirst().course().name());
        assertEquals(1, presences.size());
        assertEquals(9L, presences.getFirst().Id());
        assertEquals(4L, presences.getFirst().person().id());
        assertEquals("Ana", presences.getFirst().person().fullName());
        assertEquals(PresenceStatus.PRESENT, presences.getFirst().status());
    }

    @Test
    void shouldRejectNullClassAndPresenceResponses() {
        RecordingApiClient api = new RecordingApiClient();
        api.responses = Map.of(
                "/api/courseClass", "null",
                "/api/presence?courseId=3&courseClassId=7", "null"
        );
        ClassService service = new ClassService(api);

        CompletionException classesFailure = assertThrows(CompletionException.class, () -> service.findAll().join());
        assertInstanceOf(IllegalStateException.class, classesFailure.getCause());
        assertEquals("A API retornou uma lista de Aulas inválida.", classesFailure.getCause().getMessage());

        CompletionException presencesFailure = assertThrows(CompletionException.class,
                () -> service.findAllPresences(3L, 7L).join());
        assertInstanceOf(IllegalStateException.class, presencesFailure.getCause());
        assertEquals("A API retornou uma lista de presenças inválida.", presencesFailure.getCause().getMessage());
    }

    @Test
    void shouldSendCorrectRequestsForClassAndPresenceChanges() {
        RecordingApiClient api = new RecordingApiClient();
        ClassService service = new ClassService(api);
        ClassDto courseClass = new ClassDto(7L, "2026-11-20", Period.NIGHT, "19:00", "21:00",
                StatusClass.ACTIVE, new CourseDto(3L, "Curso A", null, null, null, null, null));

        service.addClass("2026-11-20", Period.NIGHT, "19:00", "21:00", StatusClass.ACTIVE, 3L).join();
        service.updateClass(7L, "2026-11-21", Period.AFTERNOON, "14:00", "16:00", null, 3L).join();
        service.cancelCourse(courseClass).join();
        service.reactiveClass(courseClass).join();
        service.addPresence(4L, 7L, PresenceStatus.PRESENT).join();
        service.updatePresence(4L, 7L, PresenceStatus.JUSTIFIED).join();

        assertEquals(6, api.requests.size());
        assertRequest(api.requests.get(0), "POST", "/api/courseClass",
                "{\"day\":\"2026-11-20\",\"session\":\"NIGHT\",\"start\":\"19:00\",\"finish\":\"21:00\",\"statusClass\":\"ACTIVE\",\"courseId\":3}");
        assertRequest(api.requests.get(1), "PATCH", "/api/courseClass",
                "{\"classId\":7,\"day\":\"2026-11-21\",\"session\":\"AFTERNOON\",\"start\":\"14:00\",\"finish\":\"16:00\",\"course\":{\"id\":3}}");
        assertRequest(api.requests.get(2), "DELETE", "/api/courseClass/courseClass/7", "7");
        assertRequest(api.requests.get(3), "PATCH", "/api/courseClass/7", "7");
        assertRequest(api.requests.get(4), "POST", "/api/presence",
                "{\"personId\":4,\"courseClassId\":7,\"status\":\"PRESENT\"}");
        assertRequest(api.requests.get(5), "PATCH", "/api/presence",
                "{\"personId\":4,\"courseClassId\":7,\"status\":\"JUSTIFIED\"}");
    }

    private static void assertRequest(Request actual, String method, String path, String body) {
        assertEquals(method, actual.method());
        assertEquals(path, actual.path());
        assertEquals(JsonParser.parseString(body), JsonParser.parseString(actual.body()));
    }

    private record Request(String method, String path, String body) {}

    private static class RecordingApiClient extends ApiClient {
        Map<String, String> responses = Map.of();
        final List<Request> requests = new ArrayList<>();

        @Override public CompletableFuture<String> get(String path) {
            requests.add(new Request("GET", path, null));
            return CompletableFuture.completedFuture(responses.getOrDefault(path, "ok"));
        }

        @Override public CompletableFuture<String> post(String path, String body) {
            requests.add(new Request("POST", path, body));
            return CompletableFuture.completedFuture("ok");
        }

        @Override public CompletableFuture<String> patch(String path, String body) {
            requests.add(new Request("PATCH", path, body));
            return CompletableFuture.completedFuture("ok");
        }

        @Override public CompletableFuture<String> delete(String path, String body) {
            requests.add(new Request("DELETE", path, body));
            return CompletableFuture.completedFuture("ok");
        }
    }
}

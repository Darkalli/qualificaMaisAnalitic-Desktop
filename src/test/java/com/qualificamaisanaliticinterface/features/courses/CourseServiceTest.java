package com.qualificamaisanaliticinterface.features.courses;

import com.google.gson.JsonParser;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class CourseServiceTest {
    @Test
    void findAllParsesCoursesFromApi() {
        RecordingApiClient api = new RecordingApiClient();
        api.response = "[{\"id\":7,\"name\":\"Curso A\",\"description\":\"Descrição\",\"start\":\"2026-11-10\",\"finish\":\"2026-11-20\",\"statusCourse\":\"ACTIVE\",\"period\":\"AFTERNOON\"}]";

        var courses = new CourseService(api).findAll().join();

        assertEquals("/api/course", api.path);
        assertEquals(1, courses.size());
        assertEquals(new CourseDto(7L, "Curso A", "Descrição", "2026-11-10", "2026-11-20", StatusCourse.ACTIVE, Period.AFTERNOON), courses.getFirst());
    }

    @Test
    void findAllRejectsNullResponse() {
        RecordingApiClient api = new RecordingApiClient();
        api.response = "null";

        CompletionException failure = assertThrows(CompletionException.class, () -> new CourseService(api).findAll().join());

        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals("A API retornou uma lista de cursos inválida.", failure.getCause().getMessage());
    }

    @Test
    void addCourseSendsAllFields() {
        RecordingApiClient api = new RecordingApiClient();
        new CourseService(api).addCourse("Curso A", "Descrição", "2026-11-10", "2026-11-20", StatusCourse.ACTIVE, Period.AFTERNOON).join();

        assertEquals("POST", api.method);
        assertEquals("/api/course", api.path);
        var body = JsonParser.parseString(api.body).getAsJsonObject();
        assertEquals("Curso A", body.get("name").getAsString());
        assertEquals("Descrição", body.get("description").getAsString());
        assertEquals("2026-11-10", body.get("start").getAsString());
        assertEquals("2026-11-20", body.get("finish").getAsString());
        assertEquals("ACTIVE", body.get("statusCourse").getAsString());
        assertEquals("AFTERNOON", body.get("period").getAsString());
    }

    @Test
    void updateCourseSendsIdAndChangedFields() {
        RecordingApiClient api = new RecordingApiClient();
        new CourseService(api).updateCourse(7L, "Novo nome", "Nova descrição", "2026-12-01", "2026-12-20", StatusCourse.ACTIVE, Period.NIGHT).join();

        assertEquals("PATCH", api.method);
        assertEquals("/api/course", api.path);
        var body = JsonParser.parseString(api.body).getAsJsonObject();
        assertEquals(7L, body.get("courseId").getAsLong());
        assertEquals("Novo nome", body.get("name").getAsString());
        assertEquals("Nova descrição", body.get("description").getAsString());
        assertEquals("2026-12-01", body.get("start").getAsString());
        assertEquals("2026-12-20", body.get("finish").getAsString());
        assertEquals("ACTIVE", body.get("statusCourse").getAsString());
        assertEquals("NIGHT", body.get("period").getAsString());
    }

    @Test
    void cancelCourseUsesDeleteEndpoint() {
        RecordingApiClient api = new RecordingApiClient();
        new CourseService(api).cancelCourse(course()).join();

        assertEquals("DELETE", api.method);
        assertEquals("/api/course/course/7", api.path);
        assertEquals("7", api.body);
    }

    @Test
    void reactivateCourseUsesPatchEndpoint() {
        RecordingApiClient api = new RecordingApiClient();
        new CourseService(api).reactiveCourse(course()).join();

        assertEquals("PATCH", api.method);
        assertEquals("/api/course/7", api.path);
        assertEquals("7", api.body);
    }

    private static CourseDto course() {
        return new CourseDto(7L, "Curso A", "Descrição", "2026-11-10", "2026-11-20", StatusCourse.ACTIVE, Period.AFTERNOON);
    }

    private static class RecordingApiClient extends ApiClient {
        String method;
        String path;
        String body;
        String response = "ok";

        @Override public CompletableFuture<String> get(String path) {
            this.method = "GET";
            this.path = path;
            return CompletableFuture.completedFuture(response);
        }

        @Override public CompletableFuture<String> post(String path, String body) {
            this.method = "POST";
            this.path = path;
            this.body = body;
            return CompletableFuture.completedFuture(response);
        }

        @Override public CompletableFuture<String> patch(String path, String body) {
            this.method = "PATCH";
            this.path = path;
            this.body = body;
            return CompletableFuture.completedFuture(response);
        }

        @Override public CompletableFuture<String> delete(String path, String body) {
            this.method = "DELETE";
            this.path = path;
            this.body = body;
            return CompletableFuture.completedFuture(response);
        }
    }
}

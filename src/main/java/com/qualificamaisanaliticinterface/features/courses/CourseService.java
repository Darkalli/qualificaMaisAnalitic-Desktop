package com.qualificamaisanaliticinterface.features.courses;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.courses.dto.AddCourseRequest;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.courses.dto.UpdateCourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CourseService {
    private final ApiClient apiClient = new ApiClient();
    private final Gson gson = new Gson();

    public CompletableFuture<List<CourseDto>> findAll() {
        return apiClient.get("/api/course").thenApply(json -> {
            CourseDto[] courses = gson.fromJson(json, CourseDto[].class);
            if (courses == null) {
                throw new IllegalStateException("A API retornou uma lista de inscrições inválida.");
            }
            return List.copyOf(Arrays.asList(courses));
        });
    }

    public CompletableFuture<String> addCourse(String name, String description, String start, String finish, StatusCourse statusCourse, Period period) {
        AddCourseRequest request = new AddCourseRequest(
                name, description, start, finish, statusCourse, period);
        return apiClient.post("/api/course", gson.toJson(request));
    }

    public CompletableFuture<String> cancelCourse(CourseDto course) {
        return apiClient.delete("/api/course/course/" + course.id(), gson.toJson(course.id()));
    }

    public CompletableFuture<String> reactiveCourse(CourseDto course) {
        return apiClient.patch("/api/course/"+ course.id(), gson.toJson(course.id()));
    }

    public CompletableFuture<String> updateCourse(Long id,String name, String description, String start, String finish, StatusCourse statusCourse, Period period) {
        UpdateCourseDto request = new UpdateCourseDto(id, name, description, start, finish, statusCourse, period);
        return apiClient.patch("/api/course", gson.toJson(request));
    }
}

package com.qualificamaisanaliticinterface.features.classes;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.classes.dto.*;
import com.qualificamaisanaliticinterface.features.courses.dto.AddCourseRequest;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.courses.dto.UpdateCourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.PresenceStatus;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClassService {

    private final ApiClient apiClient = new ApiClient();
    private final Gson gson = new Gson();

    public CompletableFuture<List<ClassDto>> findAll() {
        return apiClient.get("/api/courseClass").thenApply(json -> {
            ClassDto[] classes = gson.fromJson(json, ClassDto[].class);
            if (classes == null) {
                throw new IllegalStateException("A API retornou uma lista de Aulas inválida.");
            }
            return List.copyOf(Arrays.asList(classes));
        });
    }

    public CompletableFuture<List<PresenceDto>> findAllPresences(Long courseId, Long courseClassId) {
        return apiClient.get("/api/presence?courseId=" + courseId
                + "&courseClassId=" + courseClassId).thenApply(json -> {
            PresenceDto[] classes = gson.fromJson(json, PresenceDto[].class);
            if (classes == null) {
                throw new IllegalStateException("A API retornou uma lista de presenças inválida.");
            }
            return List.copyOf(Arrays.asList(classes));
        });
    }

    public CompletableFuture<String> addClass(String day, Period period, String start, String finish, StatusClass status, Long courseId) {
        AddClassRequest request = new AddClassRequest(
                day, period, start, finish, status, courseId);
        return apiClient.post("/api/courseClass", gson.toJson(request));
    }

    public CompletableFuture<String> addPresence(Long personId, Long courseClassId, PresenceStatus status) {
        AddPresenceRequest request = new AddPresenceRequest(personId, courseClassId, status);
        return apiClient.post("/api/presence", gson.toJson(request));
    }

    public CompletableFuture<String> updatePresence(Long personId, Long courseClassId, PresenceStatus status) {
        AddPresenceRequest request = new AddPresenceRequest(personId, courseClassId, status);
        return apiClient.patch("/api/presence", gson.toJson(request));
    }

    public CompletableFuture<String> cancelCourse(ClassDto courseClass) {
        return apiClient.delete("/api/courseClass/courseClass/" + courseClass.id(), gson.toJson(courseClass.id()));
    }

    public CompletableFuture<String> reactiveClass(ClassDto classDto) {
        return apiClient.patch("/api/courseClass/"+ classDto.id(), gson.toJson(classDto.id()));
    }

    public CompletableFuture<String> updateClass(Long id,String day, Period session, String start, String finish, StatusClass statusClass, Long courseId) {
        UpdateClassDto request = new UpdateClassDto(id, day, session, start, finish, statusClass,
                new UpdateClassDto.CourseReference(courseId));
        return apiClient.patch("/api/courseClass", gson.toJson(request));
    }
}

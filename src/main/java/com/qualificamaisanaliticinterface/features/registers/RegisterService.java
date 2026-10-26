package com.qualificamaisanaliticinterface.features.registers;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.registers.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RegisterService {
    private final ApiClient apiClient = new ApiClient();
    private final Gson gson = new Gson();

    public CompletableFuture<List<RegisterDto>> findAll() {
        return apiClient.get("/api/register").thenApply(json -> {
            RegisterDto[] registers = gson.fromJson(json, RegisterDto[].class);
            if (registers == null) {
                throw new IllegalStateException("A API retornou uma lista de inscrições inválida.");
            }
            return List.copyOf(Arrays.asList(registers));
        });
    }

    public CompletableFuture<String> addRegister(String personCpf, Long courseId, String registerDate) {
        var request = new com.qualificamaisanaliticinterface.features.registers.dto.AddRegisterRequest(
                personCpf, courseId, registerDate);
        return apiClient.post("/api/register", gson.toJson(request));
    }

    public CompletableFuture<String> cancelRegister(RegisterDto register) {
        var request = new com.qualificamaisanaliticinterface.features.registers.dto.SearchRegisterRequest(
                register.person().cpf(), register.courseOfInterest().id());
        return apiClient.delete("/api/register", gson.toJson(request));
    }

    public CompletableFuture<String> reactiveRegister(RegisterDto register) {
        var request = new com.qualificamaisanaliticinterface.features.registers.dto.SearchRegisterRequest(
                register.person().cpf(), register.courseOfInterest().id());
        return apiClient.patch("/api/register/active", gson.toJson(request));
    }

    public CompletableFuture<List<CourseDto>> findCourses() {
        return apiClient.get("/api/course").thenApply(json -> {
            CourseDto[] courses = gson.fromJson(json, CourseDto[].class);
            if (courses == null) {
                throw new IllegalStateException("A API retornou uma lista de cursos inválida.");
            }
            return List.copyOf(Arrays.asList(courses));
        });
    }
}

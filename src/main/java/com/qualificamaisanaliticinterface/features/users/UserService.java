package com.qualificamaisanaliticinterface.features.users;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.courses.dto.UpdateCourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;
import com.qualificamaisanaliticinterface.features.enuns.UserRole;
import com.qualificamaisanaliticinterface.features.registers.dto.AddRegisterRequest;
import com.qualificamaisanaliticinterface.features.registers.dto.RegisterDto;
import com.qualificamaisanaliticinterface.features.users.dto.AddUserDto;
import com.qualificamaisanaliticinterface.features.users.dto.UpdateUserDto;
import com.qualificamaisanaliticinterface.features.users.dto.UserDto;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UserService {

    private final ApiClient apiClient;
    private final Gson gson = new Gson();

    public UserService() {
        this(new ApiClient());
    }

    UserService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<List<UserDto>> findAll() {
        return apiClient.get("/api/auth/all").thenApply(json -> {
            UserDto[] users = gson.fromJson(json, UserDto[].class);
            if (users == null) {
                throw new IllegalStateException("A API retornou uma lista de usuários inválida.");
            }
            return List.copyOf(Arrays.asList(users));
        });
    }

    public CompletionStage<String> addUser(String user, String finalPassword, UserRole role) {
        AddUserDto request = new AddUserDto(
                user, finalPassword, role);
        return apiClient.post("/api/auth/register", gson.toJson(request));
    }

    public CompletableFuture<String> updateUser(Long id, String username, UserRole role) {
        UpdateUserDto request = new UpdateUserDto(id, username, role);
        return apiClient.patch("/api/auth", gson.toJson(request));
    }

    public CompletableFuture<String> deleteUser(Long id) {
        return apiClient.delete("/api/auth/" + id, gson.toJson(id));
    }
}

package com.qualificamaisanaliticinterface.features.people;

import com.google.gson.Gson;
import com.qualificamaisanaliticinterface.api.ApiClient;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PersonService {

    private final ApiClient apiClient;
    private final Gson gson = new Gson();

    public PersonService() {
        this(new ApiClient());
    }

    PersonService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public CompletableFuture<List<PersonDto>> findAll() {
        return apiClient.get("/api/person")
                .thenApply(json -> {
                    PersonDto[] people = gson.fromJson(
                            json,
                            PersonDto[].class
                    );

                    if (people == null) {
                        throw new IllegalStateException(
                                "A API retornou uma lista de pessoas inválida."
                        );
                    }

                    return List.copyOf(Arrays.asList(people));
                });
    }
}

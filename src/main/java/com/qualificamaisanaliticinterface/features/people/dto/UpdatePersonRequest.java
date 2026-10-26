package com.qualificamaisanaliticinterface.features.people.dto;

import com.google.gson.annotations.SerializedName;
import com.qualificamaisanaliticinterface.features.enuns.Disabilities;
import com.qualificamaisanaliticinterface.features.enuns.Education;
import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.WorkState;

import java.util.Set;

public record UpdatePersonRequest(
        @SerializedName("Cpf") String cpf,
        String socialName,
        String email,
        String personalPhone,
        String familyPhone,
        PersonDto.AddressDto address,
        Gender gender,
        Education education,
        WorkState workState,
        Set<Disabilities> disabilities
) {
}

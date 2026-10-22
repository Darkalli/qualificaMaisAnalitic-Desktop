package com.qualificamaisanaliticinterface.features.people.dto;

import com.qualificamaisanaliticinterface.features.enuns.Disabilities;
import com.qualificamaisanaliticinterface.features.enuns.Education;
import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.WorkState;

import java.util.Set;

public record PersonDto(
        String fullName,
        String socialName,
        String cpf,
        String email,
        String personalPhone,
        Boolean personalPhoneHasWhatsapp,
        String familyPhone,
        AddressDto address,
        Gender gender,
        Education education,
        WorkState workState,
        Set<Disabilities> disabilities
){
    public record AddressDto(
            Integer number,
            String street,
            String neighborhood
    ) {
    }
}

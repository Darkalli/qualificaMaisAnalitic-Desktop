package com.qualificamaisanaliticinterface.features.people.dto;

import com.qualificamaisanaliticinterface.features.enuns.Disabilities;
import com.qualificamaisanaliticinterface.features.enuns.Education;
import com.qualificamaisanaliticinterface.features.enuns.Gender;
import com.qualificamaisanaliticinterface.features.enuns.WorkState;

import java.util.Set;

public record PersonDto(
        Long id,
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

    public PersonDto(String fullName, String socialName, String cpf, String email, String personalPhone, Boolean personalPhoneHasWhatsapp, String familyPhone, AddressDto address, Gender gender, Education education, WorkState workState, Set<Disabilities> disabilities) {
        this(0L, fullName, socialName, cpf, email, personalPhone, personalPhoneHasWhatsapp, familyPhone, address, gender, education, workState, disabilities);
    }

    public PersonDto(Long id, String fullName, String socialName, String cpf, String email, String personalPhone, Boolean personalPhoneHasWhatsapp, String familyPhone, AddressDto address, Gender gender, Education education, WorkState workState, Set<Disabilities> disabilities) {
        this.id = id;
        this.fullName = fullName;
        this.socialName = socialName;
        this.cpf = cpf;
        this.email = email;
        this.personalPhone = personalPhone;
        this.personalPhoneHasWhatsapp = personalPhoneHasWhatsapp;
        this.familyPhone = familyPhone;
        this.address = address;
        this.gender = gender;
        this.education = education;
        this.workState = workState;
        this.disabilities = disabilities;
    }

    public record AddressDto(
            Integer number,
            String street,
            String neighborhood
    ) {
    }
}

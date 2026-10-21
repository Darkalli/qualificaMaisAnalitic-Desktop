package com.qualificamaisanaliticinterface.features.people.dto;

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
        String gender,
        String education,
        String workState,
        Set<String> disabilities
){
    public record AddressDto(
            Long id,
            Integer number,
            String street,
            String neighborhood
    ) {
    }
}

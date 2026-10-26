package com.qualificamaisanaliticinterface.features.registers.dto;

public record AddRegisterRequest(String personCpf, Long courseOfInterestId, String registerDate) {
}

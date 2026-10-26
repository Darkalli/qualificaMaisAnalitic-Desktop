package com.qualificamaisanaliticinterface.features.registers.dto;

import com.qualificamaisanaliticinterface.features.enuns.StatusRegister;
import com.qualificamaisanaliticinterface.features.people.dto.PersonDto;

public record RegisterDto(Long id, PersonDto person, CourseDto courseOfInterest,
                          String registerDate, StatusRegister status) {
}

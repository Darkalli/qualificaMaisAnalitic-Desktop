package com.qualificamaisanaliticinterface.features.classes.dto;

import com.qualificamaisanaliticinterface.features.courses.dto.CourseDto;
import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;

import java.time.LocalDate;
import java.time.LocalTime;

public record ClassDto(Long id, String day, Period session, String start,
                       String finish, StatusClass statusClass,
                       CourseDto course) {
}

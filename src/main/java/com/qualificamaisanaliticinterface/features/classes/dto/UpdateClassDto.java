package com.qualificamaisanaliticinterface.features.classes.dto;

import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;

public record UpdateClassDto(Long classId, String day, Period session, String start,
                             String finish, StatusClass statusClass,
                             CourseReference course) {
    public record CourseReference(Long id) { }
}

package com.qualificamaisanaliticinterface.features.courses.dto;

import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;

public record CourseDto(Long id, String name, String description, String start, String finish, StatusCourse statusCourse, Period period) {
    @Override
    public String toString() {
        return name == null ? "Curso sem nome" : name;
    }
}

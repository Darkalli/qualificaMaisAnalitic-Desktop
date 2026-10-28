package com.qualificamaisanaliticinterface.features.courses.dto;

import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;

public record UpdateCourseDto(Long courseId, String name, String description, String start, String finish, StatusCourse statusCourse, Period period) {
}

package com.qualificamaisanaliticinterface.features.courses.dto;

import com.qualificamaisanaliticinterface.features.enuns.StatusCourse;

public record AddCourseRequest(String name, String description, String start, String finish, StatusCourse statusCourse,
                               com.qualificamaisanaliticinterface.features.enuns.Period period) {
}

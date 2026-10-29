package com.qualificamaisanaliticinterface.features.classes.dto;

import com.qualificamaisanaliticinterface.features.enuns.Period;
import com.qualificamaisanaliticinterface.features.enuns.StatusClass;

public record AddClassRequest(String day, Period session, String start,
                              String finish, StatusClass statusClass,
                              Long courseId) {
}

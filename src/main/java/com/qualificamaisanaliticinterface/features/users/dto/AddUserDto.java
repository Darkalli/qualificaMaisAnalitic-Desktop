package com.qualificamaisanaliticinterface.features.users.dto;

import com.qualificamaisanaliticinterface.features.enuns.UserRole;

public record AddUserDto(String username, String password, UserRole role) {
}

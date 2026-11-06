package com.qualificamaisanaliticinterface.features.users.dto;

import com.qualificamaisanaliticinterface.features.enuns.UserRole;

public record UpdateUserDto(Long id, String username, UserRole role) {
}

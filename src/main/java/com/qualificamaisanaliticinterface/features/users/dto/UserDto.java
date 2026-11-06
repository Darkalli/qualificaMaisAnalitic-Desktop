package com.qualificamaisanaliticinterface.features.users.dto;

import com.qualificamaisanaliticinterface.features.enuns.UserRole;

public record UserDto(Long id, String username, UserRole role) {
}

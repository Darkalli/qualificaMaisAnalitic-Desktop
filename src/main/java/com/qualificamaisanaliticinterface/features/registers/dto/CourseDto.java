package com.qualificamaisanaliticinterface.features.registers.dto;

public record CourseDto(Long id, String name, String description, String start, String finish) {
    @Override
    public String toString() {
        return name == null ? "Curso sem nome" : name;
    }
}

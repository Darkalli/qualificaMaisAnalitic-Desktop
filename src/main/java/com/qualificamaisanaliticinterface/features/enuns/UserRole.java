package com.qualificamaisanaliticinterface.features.enuns;

public enum UserRole {
    ADMIN("Admin"), AGENT("Agente");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getDescription();
    }
}

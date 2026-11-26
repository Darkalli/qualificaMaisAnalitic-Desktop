package com.qualificamaisanaliticinterface.features.enuns;

public enum StatusRegister {
    ACTIVE("Ativa"), CANCELED("Cancelada"), WAITLISTED("Reserva");

    private final String description;

    StatusRegister(String description) {
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

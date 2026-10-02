package com.tienda.model;

public enum Role {
    ADMINISTRADOR("Administrador"),
    CLIENTE("Cliente"),
    AUDITOR("Auditor");

    private final String label;

    Role(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}

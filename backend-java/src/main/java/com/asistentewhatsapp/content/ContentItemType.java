package com.asistentewhatsapp.content;

public enum ContentItemType {
    CATEGORY("Categoría"),
    SERVICE("Servicio"),
    LANDING_PAGE("Landing page");

    private final String label;

    ContentItemType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ContentItemType fromLabel(String label) {
        for (ContentItemType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de contenido no valido: " + label);
    }
}
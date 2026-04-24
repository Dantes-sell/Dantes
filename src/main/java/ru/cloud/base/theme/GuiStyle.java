package ru.cloud.base.theme;

public enum GuiStyle {
    MINIMALISM("Minimalism"),
    LIQUID_GLASS("Liquid Glass");

    private final String displayName;

    GuiStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

package com.covemanager;

public class Category {
    private String name;
    private int icon;
    private int color;

    public Category(String name, int icon, int color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }
}
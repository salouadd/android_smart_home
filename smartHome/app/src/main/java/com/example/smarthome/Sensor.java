package com.example.smarthome;

public class Sensor {
    private String name;
    private String value;
    private int iconRes;
    private boolean isDanger;
    private String room; // Nouvelle propriété

    public Sensor(String name, String value, int iconRes, boolean isDanger, String room) {
        this.name = name;
        this.value = value;
        this.iconRes = iconRes;
        this.isDanger = isDanger;
        this.room = room;
    }

    public String getName() { return name; }
    public String getValue() { return value; }
    public int getIconRes() { return iconRes; }
    public boolean isDanger() { return isDanger; }
    public String getRoom() { return room; }

    public void setValue(String value) { this.value = value; }
    public void setDanger(boolean danger) { isDanger = danger; }
}
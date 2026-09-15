package com.refind.model;

public class Location {
    private Long id;
    private String campus;
    private String building;
    private String room;
    public Location() {}
    public Location(String campus, String building, String room) { this.campus = campus; this.building = building; this.room = room; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
}

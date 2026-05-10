package com.exam.seating.model;

import com.opencsv.bean.CsvBindByName;

public class Student {

    @CsvBindByName(column = "roll_no")
    private String rollNo;

    @CsvBindByName(column = "name")
    private String name;

    @CsvBindByName(column = "branch")
    private String branch;

    @CsvBindByName(column = "semester")
    private String semester;

    @CsvBindByName(column = "room_preference")
    private String roomPreference;

    // Getters and Setters

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getRoomPreference() {
        return roomPreference;
    }

    public void setRoomPreference(String roomPreference) {
        this.roomPreference = roomPreference;
    }

    @Override
    public String toString() {
        return "Student{" +
                "rollNo='" + rollNo + '\'' +
                ", name='" + name + '\'' +
                ", branch='" + branch + '\'' +
                ", semester='" + semester + '\'' +
                ", roomPreference='" + roomPreference + '\'' +
                '}';
    }
}

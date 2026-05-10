package com.exam.seating.model;

public class SeatingPlan {
    
    private Student student;
    private String roomNumber;
    private int benchNumber;

    public SeatingPlan() {
    }

    public SeatingPlan(Student student, String roomNumber, int benchNumber) {
        this.student = student;
        this.roomNumber = roomNumber;
        this.benchNumber = benchNumber;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getBenchNumber() {
        return benchNumber;
    }

    public void setBenchNumber(int benchNumber) {
        this.benchNumber = benchNumber;
    }

    @Override
    public String toString() {
        return "SeatingPlan{" +
                "student=" + student +
                ", roomNumber='" + roomNumber + '\'' +
                ", benchNumber=" + benchNumber +
                '}';
    }
}

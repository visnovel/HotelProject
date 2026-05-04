package org.example.hotelmanager;

public class Room {
    private int id;
    private String roomNumber;
    private String category;
    private String status;
    private double price;

    public Room(int id, String roomNumber, String category, String status, double price) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.category = category;
        this.status = status;
        this.price = price;
    }

    public int getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public double getPrice() { return price; }
}
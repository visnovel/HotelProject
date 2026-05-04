package org.example.hotelmanager;

public class BookingReport {
    private int id;
    private String guestName;
    private String roomNumber;
    private String checkIn;
    private String checkOut;
    private double totalPrice;
    private String status;

    public BookingReport(int id, String guestName, String roomNumber, String checkIn, String checkOut, double totalPrice, String status) {
        this.id = id;
        this.guestName = guestName;
        this.roomNumber = roomNumber;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public int getId() { return id; }
    public String getGuestName() { return guestName; }
    public String getRoomNumber() { return roomNumber; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public double getTotalPrice() { return totalPrice; }
    public String getStatus() { return status; }
}
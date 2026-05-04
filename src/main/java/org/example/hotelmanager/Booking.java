package org.example.hotelmanager;

public class Booking {
    private int id;
    private String guestName;
    private String roomNumber;
    private String checkIn;
    private String checkOut;

    public Booking(int id, String guestName, String roomNumber, String checkIn, String checkOut) {
        this.id = id;
        this.guestName = guestName;
        this.roomNumber = roomNumber;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }
    public int getId() { return id; }
    public String getGuestName() { return guestName; }
    public String getRoomNumber() { return roomNumber; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
}
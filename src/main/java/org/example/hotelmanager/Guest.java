package org.example.hotelmanager;

public class Guest {
    private int id;
    private String fullName;
    private String phone;
    private String passport;
    private int visits;
    private double totalSpent;

    public Guest(int id, String fullName, String phone, String passport, int visits, double totalSpent) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.passport = passport;
        this.visits = visits;
        this.totalSpent = totalSpent;
    }

    public Guest(int id, String fullName, String phone, String passport, double totalSpent) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.passport = passport;
        this.totalSpent = totalSpent;
        this.visits = 0;
    }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getPassport() { return passport; }
    public int getVisits() { return visits; }
    public double getTotalSpent() { return totalSpent; }

    public double getLtv() { return totalSpent; }

    public String getLoyaltyStatus() {
        if (totalSpent >= 50000) return "VIP (10%)";
        if (totalSpent >= 20000) return "Постоянный (5%)";
        return "Новичок";
    }
}
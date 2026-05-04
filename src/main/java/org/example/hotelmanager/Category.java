package org.example.hotelmanager;

public class Category {
    private int id;
    private String name;
    private String description;
    private double price;
    private String imagePath;

    public Category(int id, String name, String description, double price, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getImagePath() { return imagePath; }
}
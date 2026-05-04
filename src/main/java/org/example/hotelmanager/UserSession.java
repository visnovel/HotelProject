package org.example.hotelmanager;

public class UserSession {
    private static UserSession instance;
    private Guest user;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void login(Guest g) {
        this.user = g;
    }

    public void logout() {
        this.user = null;
    }

    public Guest getUser() {
        return user;
    }

    public Guest getCurrentUser() {
        return user;
    }

    public boolean isLoggedIn() {
        return user != null;
    }
}
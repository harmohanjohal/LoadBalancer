package com.mycompany.javafxapplication1;

import javafx.beans.property.SimpleStringProperty;

/**
 * Represents an application user with observable JavaFX properties
 * for username, password, and role.
 */
public class User {

    /** Default role assigned when none is specified. */
    public static final String DEFAULT_ROLE = "user";

    private final SimpleStringProperty username;
    private final SimpleStringProperty role;

    public User(String username, String password, String role) {
        this.username = new SimpleStringProperty(username);
        this.role = new SimpleStringProperty(role != null ? role : DEFAULT_ROLE);
    }

    public User(String username, String password) {
        this(username, password, DEFAULT_ROLE);
    }

    public String getUsername() {
        return username.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public SimpleStringProperty usernameProperty() {
        return username;
    }

    public String getRole() {
        return role.get();
    }

    public SimpleStringProperty roleProperty() {
        return role;
    }
}

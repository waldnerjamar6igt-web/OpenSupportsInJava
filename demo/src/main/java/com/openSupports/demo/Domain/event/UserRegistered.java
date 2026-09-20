package com.openSupports.demo.Domain.event;

public class UserRegistered extends Event {
    public final String email;
    public UserRegistered(String email) { this.email = email; }
}

package com.sms.model;


import jakarta.persistence.Entity;
import java.io.Serializable;

@Entity
public class Admin extends Person implements Serializable {

    private String password;

    public Admin(String id, String name, String password) {
        super(id, name);
        this.password = password;
    }

    public String getPassword() { return password; }

    @Override
    public String getRole() {
        return "Admin";
    }
}
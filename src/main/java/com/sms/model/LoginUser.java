package model;

import java.io.Serializable;

public class LoginUser implements Serializable {

    private String id;
    private String name;
    private String password;
    private UserRole role;

    public LoginUser(String id, String name, String password, UserRole role) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }
}


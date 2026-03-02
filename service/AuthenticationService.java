package service;

import model.Admin;

public class AuthenticationService {

    private Admin admin = new Admin("admin", "Administrator", "1234");

    public boolean login(String id, String password) {
        return admin.getId().equals(id) &&
               admin.getPassword().equals(password);
    }
}
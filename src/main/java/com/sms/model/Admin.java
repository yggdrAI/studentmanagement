package model;

public class Admin extends Person {

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
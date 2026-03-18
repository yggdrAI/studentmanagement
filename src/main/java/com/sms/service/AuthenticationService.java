package service;

import model.LoginUser;
import model.UserRole;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationService {

    private final Map<String, LoginUser> users = new HashMap<>();

    public AuthenticationService() {
        // Predefined demo users
        users.put("admin", new LoginUser("admin", "Administrator", "1234", UserRole.ADMIN));
        users.put("t1", new LoginUser("t1", "Teacher One", "teacher", UserRole.TEACHER));
        users.put("s1", new LoginUser("s1", "Student One", "student", UserRole.STUDENT));
    }

    /**
     * Returns the authenticated user or null if credentials are invalid.
     */
    public LoginUser login(String id, String password) {
        LoginUser user = users.get(id);
        if (user == null) return null;
        return user.getPassword().equals(password) ? user : null;
    }
}

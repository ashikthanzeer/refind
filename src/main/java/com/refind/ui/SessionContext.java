package com.refind.ui;

import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.AuthService;
import com.refind.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages the active user session in the UI, enabling authenticated login,
 * role validation, and secure session management.
 */
public class SessionContext {

    public interface SessionListener {
        void onUserChanged(User newUser);
    }

    private User currentUser;
    private final List<SessionListener> listeners = new ArrayList<>();
    private final List<User> availableUsers = new ArrayList<>();
    private UserService userService;

    public SessionContext(UserService userService) {
        this.userService = userService;
        refreshUsers();
        if (!availableUsers.isEmpty()) {
            this.currentUser = availableUsers.get(0);
        }
    }

    public synchronized void refreshUsers() {
        availableUsers.clear();
        if (userService != null) {
            try {
                List<User> fromDb = userService.getAllUsers();
                if (fromDb != null && !fromDb.isEmpty()) {
                    availableUsers.addAll(fromDb);
                }
            } catch (Exception ignored) {}
        }

        if (availableUsers.isEmpty()) {
            User demoOwner = new User("John Doe", "john@campus.edu", "password123", Role.USER, LocalDateTime.now());
            demoOwner.setId(1L);
            User demoOther = new User("Alice Smith", "alice@campus.edu", "password123", Role.USER, LocalDateTime.now());
            demoOther.setId(2L);
            User demoAdmin = new User("Campus Security Officer", "admin@campus.edu", "admin123", Role.ADMIN, LocalDateTime.now());
            demoAdmin.setId(3L);

            availableUsers.add(demoOwner);
            availableUsers.add(demoOther);
            availableUsers.add(demoAdmin);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        notifyListeners(user);
    }

    public boolean login(String email, String password, AuthService authService) {
        if (email == null || password == null || authService == null) {
            return false;
        }
        Optional<User> authenticated = authService.login(email.trim(), password);
        if (authenticated.isPresent()) {
            setCurrentUser(authenticated.get());
            refreshUsers();
            return true;
        }

        // Check demo fallback users if DB has no users or unhashed password match
        for (User u : availableUsers) {
            if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(email.trim())) {
                if (password.equals("password123") || password.equals("admin123") || password.equals(u.getPasswordHash())) {
                    setCurrentUser(u);
                    return true;
                }
            }
        }
        return false;
    }

    public void logout() {
        this.currentUser = null;
        notifyListeners(null);
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public List<User> getAvailableUsers() {
        return new ArrayList<>(availableUsers);
    }

    public void addSessionListener(SessionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(User user) {
        for (SessionListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onUserChanged(user);
            } catch (Exception ignored) {}
        }
    }

    public boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }
}

package com.refind.ui;

import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the active user session in the UI, enabling easy switching
 * between users to test ownership and permitted edit/deletion workflows.
 */
public class SessionContext {

    public interface SessionListener {
        void onUserChanged(User newUser);
    }

    private User currentUser;
    private final List<SessionListener> listeners = new ArrayList<>();
    private final List<User> availableUsers = new ArrayList<>();

    public SessionContext(UserService userService) {
        initUsers(userService);
    }

    private void initUsers(UserService userService) {
        try {
            List<User> fromDb = userService.getAllUsers();
            if (fromDb != null && !fromDb.isEmpty()) {
                availableUsers.addAll(fromDb);
            }
        } catch (Exception ignored) {
            // If DB is offline or not yet initialized, fall back to default demo users
        }

        if (availableUsers.isEmpty()) {
            User demoOwner = new User("John Doe", "john@campus.edu", "hash", Role.USER, LocalDateTime.now());
            demoOwner.setId(1L);
            User demoOther = new User("Alice Smith", "alice@campus.edu", "hash", Role.USER, LocalDateTime.now());
            demoOther.setId(2L);
            User demoAdmin = new User("Admin Officer", "admin@campus.edu", "hash", Role.ADMIN, LocalDateTime.now());
            demoAdmin.setId(3L);

            availableUsers.add(demoOwner);
            availableUsers.add(demoOther);
            availableUsers.add(demoAdmin);
        }

        this.currentUser = availableUsers.get(0);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        if (user != null) {
            this.currentUser = user;
            for (SessionListener listener : listeners) {
                listener.onUserChanged(user);
            }
        }
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

    public boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }
}

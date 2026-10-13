package com.refind.ui;

import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the active user session in the UI, enforcing role-based permissions,
 * authentication state, and notifying UI listeners of login/logout transitions.
 */
public class SessionContext {

    public interface SessionListener {
        void onUserChanged(User newUser);
    }

    private User currentUser;
    private final List<SessionListener> listeners = new ArrayList<>();

    /** Creates an unauthenticated session. */
    public SessionContext() {
        // A session is populated only by a successful login or registration.
    }

    /**
     * Retained for callers that previously supplied a user service. User lists must
     * not be loaded into a session because they enabled post-login impersonation.
     */
    public SessionContext(UserService userService) {
        this();
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User requireAuthenticated() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("Authentication required. Please sign in to continue.");
        }
        return currentUser;
    }

    public void requireAdmin() {
        User user = requireAuthenticated();
        if (user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Administrator access is required.");
        }
    }

    public boolean canAccessUserData(Long targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        if (currentUser == null) {
            return false;
        }
        return currentUser.getRole() == Role.ADMIN || targetUserId.equals(currentUser.getId());
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Records the identity established by the login, registration, or profile UI.
     * Package visibility prevents unrelated application layers from changing it.
     */
    void setCurrentUser(User user) {
        this.currentUser = user;
        notifyListeners();
    }

    public void logout() {
        this.currentUser = null;
        notifyListeners();
    }

    public void addSessionListener(SessionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (SessionListener listener : new ArrayList<>(listeners)) {
            listener.onUserChanged(currentUser);
        }
    }

    public boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }
}

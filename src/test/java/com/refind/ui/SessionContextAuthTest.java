package com.refind.ui;

import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.UserService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SessionContextAuthTest {

    private static final UserService EMPTY_USER_SERVICE = new UserService() {
        @Override public Optional<User> getUserById(Long id) { return Optional.empty(); }
        @Override public Optional<User> getUserByEmail(String email) { return Optional.empty(); }
        @Override public List<User> getAllUsers() { return List.of(); }
        @Override public User updateUser(User user) { return user; }
        @Override public boolean deleteUser(Long id) { return false; }
    };

    @Test
    void testSessionStartsLoggedOut() {
        SessionContext sessionContext = new SessionContext();

        assertFalse(sessionContext.isLoggedIn());
        assertNull(sessionContext.getCurrentUser());
        assertThrows(IllegalStateException.class, sessionContext::requireAuthenticated);
    }

    @Test
    void testLogoutClearsIdentityAndSubsequentLoginSetsOnlyNewIdentity() {
        SessionContext sessionContext = new SessionContext(EMPTY_USER_SERVICE);
        User firstUser = user("First User", "first@example.com", 10L, Role.USER);
        User subsequentUser = user("Subsequent User", "second@example.com", 11L, Role.USER);

        sessionContext.setCurrentUser(firstUser);
        assertSame(firstUser, sessionContext.requireAuthenticated());

        sessionContext.logout();
        assertFalse(sessionContext.isLoggedIn());
        assertNull(sessionContext.getCurrentUser());

        // This is the same transition performed by a later successful LoginDialog login.
        sessionContext.setCurrentUser(subsequentUser);
        assertSame(subsequentUser, sessionContext.requireAuthenticated());
        assertFalse(sessionContext.canAccessUserData(firstUser.getId()));
    }

    @Test
    void testSessionExposesNoAvailableUserListForPostLoginSwitching() {
        assertFalse(Arrays.stream(SessionContext.class.getMethods())
                .anyMatch(method -> method.getName().equals("getAvailableUsers")));
    }

    @Test
    void testAdminAccessAndUserOwnershipChecks() {
        SessionContext sessionContext = new SessionContext(EMPTY_USER_SERVICE);
        User user = user("Student", "student@example.com", 10L, Role.USER);
        User admin = user("Admin", "admin@example.com", 99L, Role.ADMIN);

        sessionContext.setCurrentUser(user);
        assertTrue(sessionContext.canAccessUserData(user.getId()));
        assertFalse(sessionContext.canAccessUserData(admin.getId()));
        assertThrows(IllegalStateException.class, sessionContext::requireAdmin);

        sessionContext.setCurrentUser(admin);
        assertDoesNotThrow(sessionContext::requireAdmin);
        assertTrue(sessionContext.canAccessUserData(user.getId()));
    }

    private static User user(String name, String email, Long id, Role role) {
        User user = new User(name, email, "hash", role, LocalDateTime.now());
        user.setId(id);
        return user;
    }
}

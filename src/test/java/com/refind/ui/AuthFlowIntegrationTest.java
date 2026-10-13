package com.refind.ui;

import com.refind.dao.UserDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.database.DatabaseConnection;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.impl.JdbcAuthService;
import com.refind.service.impl.JdbcUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class AuthFlowIntegrationTest {

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void removeUsersCreatedByThisTest() {
        UserDAO userDAO = new JdbcUserDAO();
        for (Long id : createdUserIds) {
            userDAO.deleteById(id);
        }
    }

    @Test
    void testRegistrationLoginAndLogoutFlow() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseConnection.ensureSchemaInitialized(conn);
        }

        UserDAO userDAO = new JdbcUserDAO();
        JdbcAuthService authService = new JdbcAuthService(userDAO);
        SessionContext sessionContext = new SessionContext(new JdbcUserService(userDAO));
        String email = "auth-flow-" + UUID.randomUUID() + "@example.com";

        User registered = authService.register("Auth Flow User", email, "StrongPass123");
        createdUserIds.add(registered.getId());
        assertNotNull(registered);
        assertEquals(Role.USER, registered.getRole());
        assertTrue(authService.emailExists(email));

        Optional<User> loginUser = authService.login(email, "StrongPass123");
        assertTrue(loginUser.isPresent());
        assertEquals(email, loginUser.get().getEmail());

        sessionContext.setCurrentUser(loginUser.get());
        assertTrue(sessionContext.isLoggedIn());
        assertTrue(sessionContext.canAccessUserData(loginUser.get().getId()));

        assertTrue(authService.login(email, "wrong-password").isEmpty());

        sessionContext.logout();
        assertFalse(sessionContext.isLoggedIn());
        assertThrows(IllegalStateException.class, sessionContext::requireAuthenticated);

        assertFalse(authService.login(email, "wrong-password").isPresent());
    }

    @Test
    void testPublicRegistrationCannotSelfAssignAdminRole() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseConnection.ensureSchemaInitialized(conn);
        }

        UserDAO userDAO = new JdbcUserDAO();
        JdbcAuthService authService = new JdbcAuthService(userDAO);
        String email = "admin-safety-" + UUID.randomUUID() + "@example.com";

        User created = authService.register("Normal User", email, "StrongPass123");
        createdUserIds.add(created.getId());
        assertEquals(Role.USER, created.getRole());

        User updated = authService.updateProfile(created.getId(), "Normal User", "StrongPass123", "NewStrongPass123");
        assertEquals(Role.USER, updated.getRole());
        assertNotEquals(Role.ADMIN, updated.getRole());
    }
}

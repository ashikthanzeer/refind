package com.refind.database;

import com.refind.dao.impl.JdbcClaimDAO;
import com.refind.dao.impl.JdbcItemDAO;
import com.refind.dao.impl.JdbcModerationDAO;
import com.refind.dao.impl.JdbcNotificationDAO;
import com.refind.dao.impl.JdbcUserDAO;
import com.refind.exception.ValidationException;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.impl.JdbcAuthService;
import com.refind.service.impl.JdbcModerationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class AdminBootstrapIntegrationTest {

    @Test
    void firstAdminBootstrapCreatesAdministratorAndAllowsAdminLogin() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseConnection.ensureSchemaInitialized(conn);
        }

        String email = "bootstrap-" + UUID.randomUUID() + "@campus.edu";
        JdbcAuthService authService = new JdbcAuthService(new JdbcUserDAO());

        User admin = authService.bootstrapFirstAdmin("Bootstrap Admin", email, "StrongPass123");
        assertEquals(Role.ADMIN, admin.getRole());

        Optional<User> loggedIn = authService.login(email, "StrongPass123");
        assertTrue(loggedIn.isPresent());
        assertEquals(Role.ADMIN, loggedIn.get().getRole());

        JdbcModerationService moderationService = new JdbcModerationService(
                new JdbcClaimDAO(),
                new JdbcModerationDAO(),
                new JdbcItemDAO(),
                new JdbcNotificationDAO(),
                new JdbcUserDAO());

        assertThrows(ValidationException.class, () -> moderationService.approveClaim(999999L, loggedIn.get().getId()));
    }

    @Test
    void concurrentBootstrapAttemptsCannotBothCreateTheInitialAdministrator() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseConnection.ensureSchemaInitialized(conn);
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Callable<User>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int index = i;
            final String email = "concurrent-bootstrap-" + i + "-" + UUID.randomUUID() + "@campus.edu";
            tasks.add(() -> new JdbcAuthService(new JdbcUserDAO()).bootstrapFirstAdmin("Concurrent Admin " + index, email, "StrongPass123"));
        }

        List<Future<User>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        int adminCount = 0;
        for (Future<User> future : futures) {
            try {
                User user = future.get();
                if (user != null && user.getRole() == Role.ADMIN) {
                    adminCount++;
                }
            } catch (ExecutionException ex) {
                // Exactly one attempt may legitimately fail because an admin already exists.
            }
        }

        assertEquals(1, adminCount, "Only one bootstrap attempt should create the inaugural administrator.");
    }
}

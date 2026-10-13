package com.refind.service.impl;

import com.refind.dao.NotificationDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Notification;
import com.refind.model.User;
import com.refind.model.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationAuthorizationTest {

    @Test
    void onlyTheNotificationRecipientCanMarkItRead() {
        JdbcNotificationService service = new JdbcNotificationService(new RecipientBoundNotificationDao(4L));

        assertFalse(service.markAsRead(10L, user(5L)));
        assertTrue(service.markAsRead(10L, user(4L)));
    }

    @Test
    void unauthenticatedNotificationMutationIsRejected() {
        JdbcNotificationService service = new JdbcNotificationService(new RecipientBoundNotificationDao(4L));

        assertThrows(ValidationException.class, () -> service.markAsRead(10L, null));
        assertThrows(ValidationException.class, () -> service.deleteNotification(10L, new User()));
    }

    private static User user(Long id) {
        User user = new User("Test", "test" + id + "@example.test", "hash", Role.USER, LocalDateTime.now());
        user.setId(id);
        return user;
    }

    private static final class RecipientBoundNotificationDao implements NotificationDAO {
        private final Long recipientId;

        private RecipientBoundNotificationDao(Long recipientId) { this.recipientId = recipientId; }

        @Override public Notification save(Notification notification) { return notification; }
        @Override public Optional<Notification> findById(Long id) { return Optional.empty(); }
        @Override public List<Notification> findByUser(Long userId) { return List.of(); }
        @Override public List<Notification> findUnreadByUser(Long userId) { return List.of(); }
        @Override public boolean markAsReadForUser(Long id, Long userId) { return recipientId.equals(userId); }
        @Override public boolean deleteByIdForUser(Long id, Long userId) { return recipientId.equals(userId); }
    }
}

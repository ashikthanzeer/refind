package com.refind.service.impl;

import com.refind.dao.NotificationDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Notification;
import com.refind.model.User;
import com.refind.service.NotificationService;

import java.util.List;
import java.util.Optional;

public class JdbcNotificationService implements NotificationService {
    private final NotificationDAO dao;

    public JdbcNotificationService(NotificationDAO dao) {
        this.dao = dao;
    }

    public Notification createNotification(Notification notification) {
        if (notification == null || notification.getUser() == null || notification.getUser().getId() == null) {
            throw new ValidationException("Notification user is required.");
        }
        if (notification.getText() == null || notification.getText().isBlank()) {
            throw new ValidationException("Notification text is required.");
        }
        return dao.save(notification);
    }

    public Optional<Notification> getNotificationById(Long id) { return dao.findById(id); }
    public List<Notification> getNotificationsByUser(Long id) { return dao.findByUser(id); }
    public List<Notification> getUnreadNotifications(Long id) { return dao.findUnreadByUser(id); }

    public boolean markAsRead(Long id, User currentUser) {
        return dao.markAsReadForUser(id, requireUserId(currentUser));
    }

    public boolean deleteNotification(Long id, User currentUser) {
        return dao.deleteByIdForUser(id, requireUserId(currentUser));
    }

    private Long requireUserId(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new ValidationException("Authentication required to modify notifications.");
        }
        return currentUser.getId();
    }
}

package com.refind.service.impl;

import com.refind.dao.NotificationDAO;
import com.refind.exception.ValidationException;
import com.refind.model.Notification;
import com.refind.service.NotificationService;

import java.util.List;
import java.util.Optional;

public class JdbcNotificationService implements NotificationService {

    private final NotificationDAO dao;

    public JdbcNotificationService(NotificationDAO dao) {
        this.dao = dao;
    }

    @Override
    public Notification createNotification(Notification n) {
        if (n == null || n.getUser() == null || n.getUser().getId() == null) {
            throw new ValidationException("Notification user is required.");
        }
        if (n.getText() == null || n.getText().isBlank()) {
            throw new ValidationException("Notification text is required.");
        }
        return dao.save(n);
    }

    @Override
    public Optional<Notification> getNotificationById(Long id) {
        return dao.findById(id);
    }

    @Override
    public List<Notification> getNotificationsByUser(Long id) {
        return dao.findByUser(id);
    }

    @Override
    public List<Notification> getUnreadNotifications(Long id) {
        return dao.findUnreadByUser(id);
    }

    @Override
    public boolean markAsRead(Long id) {
        return dao.markAsRead(id);
    }

    @Override
    public boolean deleteNotification(Long id) {
        return dao.deleteById(id);
    }
}

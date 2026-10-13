package com.refind.service;

import com.refind.model.Notification;
import com.refind.model.User;
import java.util.List;
import java.util.Optional;

public interface NotificationService {
    Notification createNotification(Notification notification);
    Optional<Notification> getNotificationById(Long id);
    List<Notification> getNotificationsByUser(Long userId);
    List<Notification> getUnreadNotifications(Long userId);
    boolean markAsRead(Long id, User currentUser);
    boolean deleteNotification(Long id, User currentUser);
}

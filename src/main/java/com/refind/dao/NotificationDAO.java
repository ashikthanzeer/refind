package com.refind.dao;

import com.refind.model.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationDAO {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findByUser(Long userId);
    List<Notification> findUnreadByUser(Long userId);
    boolean markAsRead(Long id);
    boolean deleteById(Long id);
}

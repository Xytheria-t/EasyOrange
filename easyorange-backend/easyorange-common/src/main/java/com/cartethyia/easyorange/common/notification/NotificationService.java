package com.cartethyia.easyorange.common.notification;

import java.util.Map;

public interface NotificationService {

    void notify(Notification notification);

    record Notification(
            String recipient,
            NotificationType type,
            String title,
            String content,
            Map<String, Object> metadata
    ) {
        public static Notification email(String to, String subject, String content) {
            return new Notification(to, NotificationType.EMAIL, subject, content, Map.of());
        }
    }

    enum NotificationType {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
}
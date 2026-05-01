package com.cartethyia.easyorange.common.notification;

import java.util.Map;

public interface NotificationService {

    void notify(Notification notification);

    void sendEmail(String to, String subject, String content);

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

        public static Notification sms(String phone, String content) {
            return new Notification(phone, NotificationType.SMS, null, content, Map.of());
        }

        public static Notification push(String userId, String title, String content) {
            return new Notification(userId, NotificationType.PUSH, title, content, Map.of());
        }

        public static Notification inApp(String userId, String title, String content) {
            return new Notification(userId, NotificationType.IN_APP, title, content, Map.of());
        }
    }

    enum NotificationType {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }
}

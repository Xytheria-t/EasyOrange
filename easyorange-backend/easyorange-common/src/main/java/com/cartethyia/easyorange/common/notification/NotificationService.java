package com.cartethyia.easyorange.common.notification;

public interface NotificationService {

    void sendEmail(String to, String subject, String content);
}

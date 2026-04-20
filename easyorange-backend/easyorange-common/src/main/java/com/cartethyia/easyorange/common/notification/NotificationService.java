package com.cartethyia.easyorange.common.notification;

public interface NotificationService {

    boolean sendEmail(String to, String subject, String content);
}

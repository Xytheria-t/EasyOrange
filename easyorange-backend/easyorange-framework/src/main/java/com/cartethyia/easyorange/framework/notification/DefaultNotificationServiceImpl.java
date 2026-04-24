package com.cartethyia.easyorange.framework.notification;

import com.cartethyia.easyorange.common.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultNotificationServiceImpl implements NotificationService {

    @Override
    public void sendEmail(String to, String subject, String content) {
        log.info("[邮件发送] 收件人: {}, 主题: {}", to, subject);
    }
}

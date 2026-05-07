package com.cartethyia.easyorange.framework.notification;

import com.cartethyia.easyorange.common.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultNotificationServiceImpl implements NotificationService {

    @Override
    public void notify(Notification notification) {
        switch (notification.type()) {
            case EMAIL -> sendEmail(notification.recipient(), notification.title(), notification.content());
            case SMS -> log.info("[SMS发送] 收件人: {}, 内容: {}", notification.recipient(), notification.content());
            case PUSH -> log.info("[PUSH推送] 用户: {}, 标题: {}, 内容: {}", notification.recipient(), notification.title(), notification.content());
            case IN_APP -> log.info("[站内信] 用户: {}, 标题: {}, 内容: {}", notification.recipient(), notification.title(), notification.content());
        }
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        log.info("[邮件发送] 收件人: {}, 主题: {}", to, subject);
    }
}
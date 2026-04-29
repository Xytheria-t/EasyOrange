package com.cartethyia.easyorange.user.event.extractor;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.event.EventExtractor;
import com.cartethyia.easyorange.user.event.PasswordChangedEvent;
import org.springframework.stereotype.Component;

@Component("passwordChangedEventExtractor")
public class PasswordChangedEventExtractor implements EventExtractor<Long, PasswordChangedEvent> {

    @Override
    public PasswordChangedEvent extract(Long result) {
        Long userId = SecurityContextUtil.getCurrentUserIdOrThrow();
        return new PasswordChangedEvent(userId);
    }
}
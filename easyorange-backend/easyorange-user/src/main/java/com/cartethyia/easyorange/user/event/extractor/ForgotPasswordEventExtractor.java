package com.cartethyia.easyorange.user.event.extractor;

import com.cartethyia.easyorange.user.event.EventExtractor;
import com.cartethyia.easyorange.user.event.PasswordChangedEvent;
import org.springframework.stereotype.Component;

@Component("forgotPasswordEventExtractor")
public class ForgotPasswordEventExtractor implements EventExtractor<Long, PasswordChangedEvent> {

    @Override
    public PasswordChangedEvent extract(Long userId) {
        return new PasswordChangedEvent(userId);
    }
}

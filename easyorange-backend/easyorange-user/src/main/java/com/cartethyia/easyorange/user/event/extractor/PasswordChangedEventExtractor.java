package com.cartethyia.easyorange.user.event.extractor;

import com.cartethyia.easyorange.user.event.EventExtractor;
import com.cartethyia.easyorange.user.event.PasswordChangedEvent;
import org.springframework.stereotype.Component;

@Component("passwordChangedEventExtractor")
public class PasswordChangedEventExtractor implements EventExtractor<Long, PasswordChangedEvent> {

    private final ThreadLocal<Long> userIdContext = new ThreadLocal<>();

    public void setUserId(Long userId) {
        this.userIdContext.set(userId);
    }

    @Override
    public PasswordChangedEvent extract(Long result) {
        Long userId = userIdContext.get();
        if (userId == null) {
            throw new IllegalStateException("User ID context not set. Cannot extract PasswordChangedEvent.");
        }
        try {
            return new PasswordChangedEvent(userId);
        } finally {
            userIdContext.remove();
        }
    }
}

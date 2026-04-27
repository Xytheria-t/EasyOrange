package com.cartethyia.easyorange.user.event.extractor;

import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.event.EventExtractor;
import com.cartethyia.easyorange.user.event.UserRegisteredEvent;
import org.springframework.stereotype.Component;

@Component("userRegisteredEventExtractor")
public class UserRegisteredEventExtractor implements EventExtractor<Long, UserRegisteredEvent> {

    private final ThreadLocal<User> userContext = new ThreadLocal<>();

    public void setUser(User user) {
        this.userContext.set(user);
    }

    @Override
    public UserRegisteredEvent extract(Long userId) {
        User user = userContext.get();
        if (user == null) {
            throw new IllegalStateException("User context not set. Cannot extract UserRegisteredEvent.");
        }
        try {
            return new UserRegisteredEvent(userId, user.getUsername());
        } finally {
            userContext.remove();
        }
    }
}

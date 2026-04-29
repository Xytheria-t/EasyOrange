package com.cartethyia.easyorange.user.event.extractor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.event.EventExtractor;
import com.cartethyia.easyorange.user.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("userRegisteredEventExtractor")
@RequiredArgsConstructor
public class UserRegisteredEventExtractor implements EventExtractor<Long, UserRegisteredEvent> {

    private final BaseMapper<User> userMapper;

    @Override
    public UserRegisteredEvent extract(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("User not found: " + userId);
        }
        return new UserRegisteredEvent(userId, user.getUsername());
    }
}
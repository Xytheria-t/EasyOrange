package com.cartethyia.easyorange.user.adapter.outbound.query;

import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.port.UserQueryPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** {@link UserQueryPort} 实现 — 聚合 → 公开信息投影。 */
@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQueryPort {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserInfo> findAllByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllByIds(ids).stream().map(this::toUserInfo).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    private UserInfo toUserInfo(User user) {
        String nickName = null;
        String avatar = null;
        if (user.getPersonalInfo() != null) {
            nickName = user.getPersonalInfo().nickName();
            avatar = user.getPersonalInfo().avatar();
        }
        return new UserInfo(user.getId(), user.getUsername(), nickName, avatar);
    }
}

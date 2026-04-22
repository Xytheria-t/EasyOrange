package com.cartethyia.easyorange.user.application.handler;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.command.UpdateUserCommand;
import com.cartethyia.easyorange.user.domain.aggregate.UserAggregate;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.Email;
import com.cartethyia.easyorange.user.domain.valueobject.Nickname;
import com.cartethyia.easyorange.user.domain.valueobject.Phone;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateUserHandler {

    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)
    public UserVO handle(UpdateUserCommand command) {
        Long currentUserId = SecurityContextUtil.getCurrentUserIdOrThrow();

        UserAggregate user = userRepository.findById(new UserId(currentUserId))
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (command.getEmail() != null && !command.getEmail().isBlank()) {
            user.updateProfile(new Email(command.getEmail()), null, null);
        }
        if (command.getPhone() != null && !command.getPhone().isBlank()) {
            user.updateProfile(null, new Phone(command.getPhone()), null);
        }
        if (command.getGender() != null) {
            user.updateProfile(null, null, null);
        }

        userRepository.update(user);
        log.info("action=updateUserInfo success userId={}", currentUserId);

        return toUserVO(user);
    }

    private UserVO toUserVO(UserAggregate user) {
        return UserVO.builder()
                .id(user.getId().value())
                .username(user.getUsername())
                .email(user.getEmail() != null ? user.getEmail().value() : null)
                .phone(user.getPhone() != null ? user.getPhone().value() : null)
                .build();
    }
}
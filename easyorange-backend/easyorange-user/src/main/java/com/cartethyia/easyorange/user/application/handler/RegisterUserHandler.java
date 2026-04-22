package com.cartethyia.easyorange.user.application.handler;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.application.command.RegisterUserCommand;
import com.cartethyia.easyorange.user.domain.aggregate.UserAggregate;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.Password;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import com.cartethyia.easyorange.user.enums.AccountType;
import com.cartethyia.easyorange.user.enums.UserResultCode;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.enums.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserHandler {

    private final UserRepository userRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public UserId handle(RegisterUserCommand command) {
        BizRequire.isFalse(userRepository.existsByUsername(command.getUsername()),
                UserResultCode.USERNAME_EXISTS);

        Password password = Password.fromRaw(command.getPassword());
        Password encodedPassword = password.encode(passwordEncoder::encode);

        UserAggregate user = UserAggregate.builder()
                .id(new UserId(generateUserId()))
                .username(command.getUsername())
                .password(encodedPassword)
                .userType(UserType.NORMAL.getCode())
                .status(UserStatus.NORMAL.getCode())
                .loginType(AccountType.WEB.getCode())
                .build();

        userRepository.save(user);

        UserRegisteredEvent event = new UserRegisteredEvent(user.getId(), user.getUsername());
        domainEventPublisher.publish(event);

        log.info("action=register success username={}", command.getUsername());
        return user.getId();
    }

    private Long generateUserId() {
        return System.currentTimeMillis();
    }
}
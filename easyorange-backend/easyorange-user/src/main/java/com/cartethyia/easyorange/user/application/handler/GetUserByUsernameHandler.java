package com.cartethyia.easyorange.user.application.handler;

import com.cartethyia.easyorange.user.application.query.GetUserByUsernameQuery;
import com.cartethyia.easyorange.user.domain.aggregate.UserAggregate;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserByUsernameHandler {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<UserAggregate> handle(GetUserByUsernameQuery query) {
        return userRepository.findByUsername(query.getUsername());
    }
}
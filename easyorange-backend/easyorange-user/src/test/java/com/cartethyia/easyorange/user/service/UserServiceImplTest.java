package com.cartethyia.easyorange.user.service;

import com.cartethyia.easyorange.user.event.extractor.PasswordChangedEventExtractor;
import com.cartethyia.easyorange.user.event.extractor.UserRegisteredEventExtractor;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private UserRegisteredEventExtractor userRegisteredEventExtractor;

    @Mock
    private PasswordChangedEventExtractor passwordChangedEventExtractor;

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testServiceCreation() {
        assertNotNull(userService);
    }
}

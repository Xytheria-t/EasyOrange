package com.cartethyia.easyorange.user.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.assembler.UserAssembler;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.UserResultCode;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import com.cartethyia.easyorange.user.service.impl.UserServiceImpl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private UserAssembler userAssembler;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encodedpassword")
                .email("test@example.com")
                .phone("13800138000")
                .status("0")
                .userType("NORMAL")
                .loginType("WEB")
                .createTime(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("用户名不存在时注册成功")
        void register_success_whenUsernameNotExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("newuser")
                    .password("Password123")
                    .build();

            when(userService.lambdaQuery().eq(eq(User::getUsername), eq("newuser")).exists()).thenReturn(false);
            when(userService.save(any(User.class))).thenReturn(true);

            userService.register(request);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userService).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUsername()).isEqualTo("newuser");
            assertThat(savedUser.getPassword()).isNotEqualTo("Password123");
            assertThat(savedUser.getStatus()).isEqualTo("0");
        }

        @Test
        @DisplayName("用户名已存在时抛出异常")
        void register_throwsException_whenUsernameExists() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("existinguser")
                    .password("Password123")
                    .build();

            when(userService.lambdaQuery().eq(eq(User::getUsername), eq("existinguser")).exists()).thenReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(BusinessException.class);

            verify(userService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("旧密码正确时修改成功")
        void changePassword_success_withCorrectOldPassword() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .oldPassword("OldPass123")
                    .newPassword("NewPass456")
                    .build();

            when(passwordEncoder.matches("OldPass123", testUser.getPassword())).thenReturn(true);
            when(userService.getById(1L)).thenReturn(testUser);
            when(userService.lambdaUpdate().eq(eq(User::getId), eq(1L))).thenReturn(new com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.QueryWrapper<>());
            when(userService.lambdaUpdate().eq(eq(User::getId), eq(1L)).set(any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.QueryWrapper<>());
            when(userService.lambdaUpdate().eq(eq(User::getId), eq(1L)).set(any(), any()).update()).thenReturn(true);

            userService.changePassword(request);

            verify(passwordEncoder).encode("NewPass456");
        }

        @Test
        @DisplayName("旧密码错误时抛出异常")
        void changePassword_throwsException_withWrongOldPassword() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .oldPassword("WrongPassword")
                    .newPassword("NewPass456")
                    .build();

            when(userService.getById(1L)).thenReturn(testUser);
            when(passwordEncoder.matches("WrongPassword", testUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("旧密码错误");
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("手机号已注册时重置密码成功")
        void forgotPassword_success_whenPhoneExists() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .phone("13800138000")
                    .newPassword("ResetPass123")
                    .build();

            when(userService.lambdaQuery().eq(eq(User::getPhone), eq("13800138000")).one()).thenReturn(testUser);
            when(userService.lambdaUpdate().eq(eq(User::getId), eq(testUser.getId()))).thenReturn(new com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.QueryWrapper<>());
            when(userService.lambdaUpdate().eq(eq(User::getId), eq(testUser.getId())).set(any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.QueryWrapper<>());
            when(userService.lambdaUpdate().eq(eq(User::getId), eq(testUser.getId())).set(any(), any()).update()).thenReturn(true);

            userService.forgotPassword(request);

            verify(passwordEncoder).encode("ResetPass123");
        }

        @Test
        @DisplayName("手机号未注册时不抛异常静默拒绝")
        void forgotPassword_noException_whenPhoneNotExists() {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .phone("13900139000")
                    .newPassword("ResetPass123")
                    .build();

            when(userService.lambdaQuery().eq(eq(User::getPhone), eq("13900139000")).one()).thenReturn(null);

            userService.forgotPassword(request);

            verify(passwordEncoder, never()).encode(any());
            verify(userService, never()).updateById(any());
        }
    }
}

package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void shouldCallGetUserInfo() {
        // Given
        UserVO mockUserVO = new UserVO();
        mockUserVO.setId(1L);
        mockUserVO.setUsername("testuser");
        given(userService.getUserInfo()).willReturn(mockUserVO);

        // When
        var result = userController.getUserInfo();

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals("testuser", result.data().getUsername());
        verify(userService, times(1)).getUserInfo();
    }

    @Test
    void shouldCallRegister() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        given(userService.register(any(RegisterRequest.class))).willReturn(100L);

        // When
        var result = userController.register(request);

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals(100L, result.data());
        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void shouldCallUpdateUserInfo() {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("updated@example.com");
        
        UserVO updatedUserVO = new UserVO();
        updatedUserVO.setId(1L);
        updatedUserVO.setEmail("updated@example.com");
        
        given(userService.updateUserInfo(any(UpdateUserRequest.class))).willReturn(updatedUserVO);

        // When
        var result = userController.updateUserInfo(request);

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals("updated@example.com", result.data().getEmail());
        verify(userService, times(1)).updateUserInfo(any(UpdateUserRequest.class));
    }

    @Test
    void shouldCallChangePassword() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldpass");
        request.setNewPassword("newpass");
        doNothing().when(userService).changePassword(any(ChangePasswordRequest.class));

        // When
        var result = userController.changePassword(request);

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        verify(userService, times(1)).changePassword(any(ChangePasswordRequest.class));
    }

    @Test
    void shouldCallForgotPassword() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhone("1234567890");
        request.setNewPassword("newpassword");
        doNothing().when(userService).forgotPassword(any(ForgotPasswordRequest.class));

        // When
        var result = userController.forgotPassword(request);

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        verify(userService, times(1)).forgotPassword(any(ForgotPasswordRequest.class));
    }
}

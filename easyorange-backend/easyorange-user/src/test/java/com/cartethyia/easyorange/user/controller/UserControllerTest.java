package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.*;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
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

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private UserController userController;

    @Test
    void shouldCallGetCurrentUser() {
        UserProfileVO mockUserProfile = new UserProfileVO();
        mockUserProfile.setId(1L);
        mockUserProfile.setUsername("testuser");
        given(userService.getUserInfo()).willReturn(mockUserProfile);

        var result = userController.getCurrentUser();

        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals("testuser", result.data().getUsername());
        verify(userService, times(1)).getUserInfo();
    }

    @Test
    void shouldCallRegister() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        
        RegisterBo bo = new RegisterBo("newuser", "password123");
        given(userConverter.toBo(request)).willReturn(bo);
        given(userService.register(any(RegisterBo.class))).willReturn(100L);

        var result = userController.register(request);

        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals(100L, result.data());
        verify(userConverter, times(1)).toBo(request);
        verify(userService, times(1)).register(any(RegisterBo.class));
    }

    @Test
    void shouldCallUpdateUserInfo() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("updated@example.com");
        
        UpdateUserBo bo = new UpdateUserBo("updated@example.com", null, null);
        UserVO updatedUserVO = new UserVO();
        updatedUserVO.setId(1L);
        updatedUserVO.setEmail("updated@example.com");
        
        given(userConverter.toBo(request)).willReturn(bo);
        given(userService.updateUserInfo(any(UpdateUserBo.class))).willReturn(updatedUserVO);

        var result = userController.updateUserInfo(request);

        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals("updated@example.com", result.data().getEmail());
        verify(userConverter, times(1)).toBo(request);
        verify(userService, times(1)).updateUserInfo(any(UpdateUserBo.class));
    }

    @Test
    void shouldCallChangePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldpass");
        request.setNewPassword("newpass");
        
        ChangePasswordBo bo = new ChangePasswordBo("oldpass", "newpass");
        given(userConverter.toBo(request)).willReturn(bo);
        doNothing().when(userService).changePassword(any(ChangePasswordBo.class));

        var result = userController.changePassword(request);

        assertNotNull(result);
        assertEquals("A0000", result.code());
        verify(userConverter, times(1)).toBo(request);
        verify(userService, times(1)).changePassword(any(ChangePasswordBo.class));
    }

    @Test
    void shouldCallForgotPassword() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhone("1234567890");
        request.setNewPassword("newpassword");
        
        ForgotPasswordBo bo = new ForgotPasswordBo("1234567890", "newpassword");
        given(userConverter.toBo(request)).willReturn(bo);
        doNothing().when(userService).forgotPassword(any(ForgotPasswordBo.class));

        var result = userController.forgotPassword(request);

        assertNotNull(result);
        assertEquals("A0000", result.code());
        verify(userConverter, times(1)).toBo(request);
        verify(userService, times(1)).forgotPassword(any(ForgotPasswordBo.class));
    }
}

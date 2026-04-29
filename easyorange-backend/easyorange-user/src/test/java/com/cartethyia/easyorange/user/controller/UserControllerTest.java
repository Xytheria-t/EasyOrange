package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.UpdateUserBo;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.service.user.UserService;
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
}

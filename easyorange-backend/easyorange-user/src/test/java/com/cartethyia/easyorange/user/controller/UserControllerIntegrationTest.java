package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.UpdateUserBo;
import com.cartethyia.easyorange.user.dto.bo.UploadAvatarBo;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getCurrentUser_returnsUserProfile() throws Exception {
        UserService userService = mock(UserService.class);
        UserConverter userConverter = mock(UserConverter.class);

        UserProfileVO profile = UserProfileVO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        given(userService.getUserInfo()).willReturn(profile);

        UserController controller = new UserController(userService, userConverter);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void updateUserInfo_withValidRequest_returnsUpdatedUser() throws Exception {
        UserService userService = mock(UserService.class);
        UserConverter userConverter = mock(UserConverter.class);

        UserVO updatedUser = UserVO.builder()
                .id(1L)
                .username("testuser")
                .email("updated@example.com")
                .build();

        given(userConverter.toBo(any(UpdateUserRequest.class))).willReturn(new UpdateUserBo("testuser", "Test User", 1));
        given(userService.updateUserInfo(any(UpdateUserBo.class))).willReturn(updatedUser);

        UserController controller = new UserController(userService, userConverter);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateUserRequest.builder().email("updated@example.com").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"));
    }

    @Test
    void uploadAvatar_withValidFile_returnsUpdatedUser() throws Exception {
        UserService userService = mock(UserService.class);
        UserConverter userConverter = mock(UserConverter.class);

        UserVO updatedUser = UserVO.builder()
                .id(1L)
                .username("testuser")
                .build();

        given(userConverter.toBo(any(MockMultipartFile.class))).willReturn(new UploadAvatarBo(null));
        given(userService.uploadAvatar(any(UploadAvatarBo.class))).willReturn(updatedUser);

        UserController controller = new UserController(userService, userConverter);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "test.jpg", "image/jpeg", "fake-image-data".getBytes());

        mockMvc.perform(multipart("/api/users/avatar")
                        .file(avatar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }
}
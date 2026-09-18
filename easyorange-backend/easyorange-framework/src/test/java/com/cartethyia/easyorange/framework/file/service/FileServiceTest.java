package com.cartethyia.easyorange.framework.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.entity.UploadFileDO;
import com.cartethyia.easyorange.framework.file.mapper.UploadFileMapper;
import com.cartethyia.easyorange.framework.file.storage.FileStorage;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * FileService 上传落库 — 单元测试。
 * <p>
 * 重点：{@code BaseDO.id} 是 {@code IdType.INPUT}，ID 必须由应用层生成；
 * 漏赋值不会在编译期报错，只会在 insert 时以 {@code Column 'id' cannot be null} 暴露（曾经如此）。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 上传落库")
class FileServiceTest {

    private static final String GENERATED_ID = "01a0b00f-0000-7000-8000-000000000001";

    /** 1×1 真实 PNG —— 走 FileUtils 的魔数校验，假字节会被拒 */
    private static final byte[] PNG_1PX = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==");

    @Mock
    private UploadFileMapper uploadFileMapper;

    @Mock
    private FileStorage fileStorage;

    private final IdGenerator idGenerator = () -> GENERATED_ID;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private FileService newService() {
        return new FileService(uploadFileMapper, fileStorage, idGenerator);
    }

    private void loginAs(String userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(new AuthUser(userId, "testuser"), null, List.of()));
    }

    @Test
    @DisplayName("上传：ID 由 IdGenerator 生成后落库（漏赋值即 NOT NULL 插入失败）")
    void uploadFile_assignsGeneratedIdBeforeInsert() throws Exception {
        loginAs("1001");
        when(fileStorage.store(any(), any(), any())).thenReturn("2026/09/17/abc.png");
        when(fileStorage.getUrl(any())).thenReturn("/api/file/abc/view");

        var file = new MockMultipartFile("file", "photo.png", "image/png", PNG_1PX);
        UploadFileVO vo = newService().uploadFile(file, "product");

        var captor = ArgumentCaptor.forClass(UploadFileDO.class);
        verify(uploadFileMapper).insert(captor.capture());
        UploadFileDO persisted = captor.getValue();

        assertThat(persisted.getId()).isEqualTo(GENERATED_ID);
        assertThat(persisted.getUploaderId()).isEqualTo("1001");
        assertThat(persisted.getFileName()).isEqualTo("photo.png");
        assertThat(persisted.getFileType()).isEqualTo("png");
        assertThat(persisted.getStorageKey()).isEqualTo("2026/09/17/abc.png");
        assertThat(vo.id()).isEqualTo(GENERATED_ID);
    }

    @Test
    @DisplayName("未登录：拒绝上传且不触碰存储与 DB")
    void uploadFile_withoutLogin_rejectsBeforeStorage() {
        var file = new MockMultipartFile("file", "photo.png", "image/png", PNG_1PX);

        assertThatThrownBy(() -> newService().uploadFile(file, "product", "2001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未登录");

        verify(uploadFileMapper, never()).insert(any(UploadFileDO.class));
    }
}

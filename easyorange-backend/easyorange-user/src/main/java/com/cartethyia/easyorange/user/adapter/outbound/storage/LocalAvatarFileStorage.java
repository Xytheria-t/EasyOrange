package com.cartethyia.easyorange.user.adapter.outbound.storage;

import com.cartethyia.easyorange.framework.util.FileUtils;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.service.FileService;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件系统实现的头像存储适配器
 * <p>
 * 职责：
 * <ul>
 *   <li>实现 {@link AvatarFilePort} 接口</li>
 *   <li>处理文件类型转换（MultipartFile → byte[]）</li>
 *   <li>调用框架层的文件服务</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalAvatarFileStorage implements AvatarFilePort {

    private static final String AVATAR_BUSINESS_TYPE = "user_avatar";

    private final FileService fileService;

    @Override
    public String upload(byte[] content, String contentType, String originalFilename, Long userId) {
        MultipartFile multipartFile = new ByteArrayMultipartFile(content, contentType, originalFilename);
        UploadFileVO result = fileService.uploadFile(multipartFile, AVATAR_BUSINESS_TYPE, userId);
        return result.getFileUrl();
    }

    @Override
    public void deleteIfExists(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }
        try {
            String relativePath = avatarUrl.replace("/api/file/", "").replace("\\", "/");
            FileUtils.deleteFile(relativePath);
        } catch (Exception e) {
            log.warn("删除头像文件失败: {}", avatarUrl, e);
        }
    }

    /**
     * 字节数组转 MultipartFile 适配器
     * <p>
     * 用于将领域层的 byte[] 转换为框架层需要的 MultipartFile
     */
    private static class ByteArrayMultipartFile implements MultipartFile {

        private final byte[] content;
        private final String contentType;
        private final String originalFilename;

        ByteArrayMultipartFile(byte[] content, String contentType, String originalFilename) {
            this.content = content;
            this.contentType = contentType;
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() {
            return "avatar";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}

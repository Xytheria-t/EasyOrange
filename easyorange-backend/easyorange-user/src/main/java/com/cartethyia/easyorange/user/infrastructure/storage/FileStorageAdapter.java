package com.cartethyia.easyorange.user.infrastructure.storage;

import com.cartethyia.easyorange.common.util.FileUtils;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageAdapter {

    private static final String AVATAR_BUSINESS_TYPE = "user_avatar";

    private final FileService fileService;

    public String uploadAvatar(MultipartFile file, Long userId) {
        UploadFileVO result = fileService.uploadFile(file, AVATAR_BUSINESS_TYPE, userId);
        return result.getFileUrl();
    }

    public void deleteIfExists(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            String relativePath = fileUrl.replace("/api/file/", "").replace("\\", "/");
            boolean deleted = FileUtils.deleteFile(relativePath);
            if (deleted) {
                log.info("action=deleteFile success, path={}", relativePath);
            }
        } catch (Exception e) {
            log.warn("action=deleteFile failed, url={}, error={}", fileUrl, e.getMessage());
        }
    }
}

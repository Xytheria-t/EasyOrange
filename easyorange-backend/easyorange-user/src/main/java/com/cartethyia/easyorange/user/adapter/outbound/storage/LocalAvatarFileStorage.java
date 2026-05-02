package com.cartethyia.easyorange.user.adapter.outbound.storage;

import com.cartethyia.easyorange.common.util.FileUtils;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.service.FileService;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class LocalAvatarFileStorage implements AvatarFilePort {

    private static final String AVATAR_BUSINESS_TYPE = "user_avatar";

    private final FileService fileService;

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        UploadFileVO result = fileService.uploadFile(file, AVATAR_BUSINESS_TYPE, userId);
        return result.getFileUrl();
    }

    @Override
    public void deleteIfExists(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            String relativePath = fileUrl.replace("/api/file/", "").replace("\\", "/");
            FileUtils.deleteFile(relativePath);
        } catch (Exception e) {
            // ignore
        }
    }
}

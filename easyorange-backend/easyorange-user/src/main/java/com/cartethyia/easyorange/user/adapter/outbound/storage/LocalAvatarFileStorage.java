package com.cartethyia.easyorange.user.adapter.outbound.storage;

import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.service.FileService;
import com.cartethyia.easyorange.framework.file.util.ByteArrayMultipartFile;
import com.cartethyia.easyorange.framework.util.FileUtils;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 本地文件系统实现的头像存储适配器。
 * <p>
 * 领域层持有 byte[]，通过 {@link ByteArrayMultipartFile} 适配到框架层的 {@link FileService}。
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class LocalAvatarFileStorage implements AvatarFilePort {

    private static final String AVATAR_BUSINESS_TYPE = "user_avatar";

    private static final Pattern AVATAR_PATH_PATTERN = Pattern.compile("^/api/file/(.+)$");

    private final FileService fileService;

    @Override
    public String upload(byte[] content, String contentType, String originalFilename, String userId) {
        var multipartFile = new ByteArrayMultipartFile(content, contentType, originalFilename);
        UploadFileVO result = fileService.uploadFile(multipartFile, AVATAR_BUSINESS_TYPE, userId);
        return result.fileUrl();
    }

    @Override
    public void deleteIfExists(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }
        var matcher = AVATAR_PATH_PATTERN.matcher(avatarUrl);
        if (!matcher.matches()) {
            log.warn("无法解析头像URL路径: {}", avatarUrl);
            return;
        }
        try {
            FileUtils.deleteFile(matcher.group(1).replace("\\", "/"));
        } catch (Exception e) {
            log.warn("删除头像文件失败: {}", avatarUrl, e);
        }
    }
}

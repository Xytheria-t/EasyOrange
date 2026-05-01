package com.cartethyia.easyorange.user.domain.port;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarFilePort {

    String uploadAvatar(MultipartFile file, Long userId);

    void deleteIfExists(String fileUrl);
}

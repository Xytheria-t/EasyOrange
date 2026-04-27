package com.cartethyia.easyorange.user.dto.bo;

import org.springframework.web.multipart.MultipartFile;

public record UploadAvatarBo(
    MultipartFile avatar
) {
    public static UploadAvatarBo from(MultipartFile avatar) {
        return new UploadAvatarBo(avatar);
    }
}

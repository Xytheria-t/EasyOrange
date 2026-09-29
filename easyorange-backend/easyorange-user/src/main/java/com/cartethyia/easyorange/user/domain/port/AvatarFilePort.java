package com.cartethyia.easyorange.user.domain.port;

/** 头像文件存储端口 — 落哪种存储由实现决定，业务侧只认返回的访问 URL，且该 URL 是后续删除的唯一句柄。 */
public interface AvatarFilePort {

    String upload(byte[] content, String contentType, String originalFilename, String userId);

    void deleteIfExists(String avatarUrl);
}

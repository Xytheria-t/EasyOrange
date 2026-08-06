package com.cartethyia.easyorange.user.domain.port;

/**
 * 头像文件存储端口
 * <p>
 * 职责：
 * <ul>
 *   <li>头像文件的上传和存储</li>
 *   <li>旧头像文件的删除</li>
 * </ul>
 *
 * <p>实现类可以基于本地文件系统、OSS（阿里云 OSS、AWS S3 等）等存储方式
 */
public interface AvatarFilePort {

    /**
     * 上传头像
     *
     * @param content 文件内容
     * @param contentType 内容类型 (如 "image/jpeg", "image/png")
     * @param originalFilename 原始文件名
     * @param userId 用户ID
     * @return 头像访问URL
     */
    String upload(byte[] content, String contentType, String originalFilename, String userId);

    /**
     * 删除头像（如果存在）
     *
     * @param avatarUrl 头像URL
     */
    void deleteIfExists(String avatarUrl);
}

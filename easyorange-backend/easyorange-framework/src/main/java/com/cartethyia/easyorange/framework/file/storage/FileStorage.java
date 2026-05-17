package com.cartethyia.easyorange.framework.file.storage;

import java.io.IOException;

/**
 * Pluggable file storage abstraction.
 * <p>
 * Supports multiple backends: local filesystem, S3-compatible object storage,
 * Aliyun OSS, etc. Each backend implements this interface and is registered
 * as a Spring bean. The active implementation is selected via configuration.
 * </p>
 */
public interface FileStorage {

    /**
     * Store a file and return a storage identifier.
     *
     * @param content          file byte content
     * @param originalFilename original file name (used for extension detection)
     * @param contentType      MIME type (used as fallback for extension detection)
     * @return storage identifier (relative path for local, object key for cloud)
     * @throws IOException if the storage operation fails
     */
    String store(byte[] content, String originalFilename, String contentType) throws IOException;

    /**
     * Load file content by its storage identifier.
     *
     * @param identifier storage identifier returned by {@link #store}
     * @return file byte content
     * @throws IOException if the file is not found or cannot be read
     */
    byte[] load(String identifier) throws IOException;

    /**
     * Delete a stored file by its storage identifier.
     *
     * @param identifier storage identifier returned by {@link #store}
     * @throws IOException if the deletion operation fails
     */
    void delete(String identifier) throws IOException;

    /**
     * Get the externally accessible URL for a stored file.
     * <p>
     * For local storage, this returns a relative API path (e.g., {@code /api/file/2026/05/17/uuid.jpg}).
     * For cloud storage, this returns a full CDN URL (e.g., {@code https://cdn.example.com/objects/uuid.jpg}).
     * </p>
     *
     * @param identifier storage identifier returned by {@link #store}
     * @return accessible URL string
     */
    String getUrl(String identifier);

    /**
     * Whether files stored by this backend can be accessed directly via URL.
     * <p>
     * Local filesystem storage returns {@code false} because files are served through
     * the API controller. Cloud storage (S3/OSS) returns {@code true} because the
     * CDN URL is directly accessible.
     * </p>
     */
    boolean supportsDirectUrl();
}

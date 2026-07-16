package com.cartethyia.easyorange.framework.file.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * byte[] → {@link MultipartFile} 适配器。
 * <p>
 * 当领域层持有 byte[] 而框架层需要 {@link MultipartFile} 时，
 * 使用此适配器避免依赖 Servlet 容器或测试框架的 MockMultipartFile。
 */
public class ByteArrayMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String contentType;
    private final String originalFilename;

    public ByteArrayMultipartFile(byte[] content, String contentType, String originalFilename) {
        this.content = content;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
    }

    @Override
    public String getName() {
        return "file";
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
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        try (var out = new FileOutputStream(dest)) {
            out.write(content);
        }
    }
}

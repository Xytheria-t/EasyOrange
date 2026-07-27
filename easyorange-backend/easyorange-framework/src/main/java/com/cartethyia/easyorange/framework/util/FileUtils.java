package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.common.exception.file.FileException;
import com.cartethyia.easyorange.common.exception.file.FileSizeLimitExceededException;
import com.cartethyia.easyorange.common.exception.file.InvalidExtensionException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FileUtils {

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    public static final long DEFAULT_MAX_SIZE = 50 * MB;

    public static final Set<String> DEFAULT_ALLOWED_EXTENSION = new LinkedHashSet<>(Set.of(
            "bmp", "gif", "jpg", "jpeg", "png", "webp",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "rar", "zip", "gz", "bz2", "pdf"
    ));

    private static final Map<String, String> MIME_TO_EXTENSION = Map.ofEntries(
            Map.entry("image/bmp", "bmp"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/jpg", "jpg"),
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/webp", "webp"),
            Map.entry("application/pdf", "pdf"),
            Map.entry("application/msword", "doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
            Map.entry("application/vnd.ms-powerpoint", "ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
            Map.entry("application/x-rar-compressed", "rar"),
            Map.entry("application/zip", "zip"),
            Map.entry("application/x-gzip", "gz"),
            Map.entry("application/x-bzip2", "bz2"),
            Map.entry("video/mp4", "mp4"),
            Map.entry("video/x-msvideo", "avi"),
            Map.entry("video/quicktime", "mov"),
            Map.entry("video/x-ms-wmv", "wmv"),
            Map.entry("video/x-matroska", "mkv"),
            Map.entry("video/webm", "webm"),
            Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/wav", "wav"),
            Map.entry("audio/x-ms-wma", "wma")
    );

    private static final Map<String, byte[]> FILE_MAGIC_NUMBERS = Map.ofEntries(
            Map.entry("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
            Map.entry("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}),
            Map.entry("gif", new byte[]{0x47, 0x49, 0x46, 0x38}),
            Map.entry("bmp", new byte[]{0x42, 0x4D}),
            Map.entry("webp", new byte[]{0x52, 0x49, 0x46, 0x46}),
            Map.entry("pdf", new byte[]{0x25, 0x50, 0x44, 0x46}),
            Map.entry("zip", new byte[]{0x50, 0x4B, 0x03, 0x04}),
            Map.entry("rar", new byte[]{0x52, 0x61, 0x72, 0x21}),
            Map.entry("doc", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}),
            Map.entry("ppt", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}),
            Map.entry("xlsx", new byte[]{0x50, 0x4B, 0x03, 0x04}),
            Map.entry("xls", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0})
    );

    private FileUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void assertAllowed(MultipartFile file, Collection<String> allowedExtension) {
        var size = file.getSize();
        if (size > DEFAULT_MAX_SIZE) {
            throw new FileSizeLimitExceededException(DEFAULT_MAX_SIZE, size);
        }
        if (allowedExtension == null || allowedExtension.isEmpty()) {
            return;
        }
        var extension = getExtension(file);
        var allowedList = allowedExtension instanceof List<String> list
                ? list : new ArrayList<>(allowedExtension);
        if (!isAllowedExtension(extension, allowedList)) {
            throw new InvalidExtensionException(allowedList, extension, file.getOriginalFilename());
        }
        assertFileMagicNumber(file, extension, allowedList);
    }

    private static void assertFileMagicNumber(MultipartFile file, String extension, List<String> allowedExtensions) {
        var expectedMagic = FILE_MAGIC_NUMBERS.get(extension.toLowerCase());
        if (expectedMagic == null) return;
        try (var in = file.getInputStream()) {
            var header = in.readNBytes(expectedMagic.length);
            if (header.length < expectedMagic.length) {
                throw new InvalidExtensionException(allowedExtensions, extension, file.getOriginalFilename());
            }
            var mm = Arrays.mismatch(expectedMagic, header);
            if (mm != -1 && mm != expectedMagic.length) {
                throw new InvalidExtensionException(allowedExtensions, extension, file.getOriginalFilename());
            }
        } catch (IOException e) {
            throw FileException.of("文件读取失败：" + file.getOriginalFilename(), e);
        }
    }

    private static boolean isAllowedExtension(String extension, Collection<String> allowedExtension) {
        if (allowedExtension == null || allowedExtension.isEmpty()) {
            return true;
        }
        return allowedExtension.stream()
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }

    public static String getExtension(MultipartFile file) {
        var filename = file.getOriginalFilename();
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        var contentType = file.getContentType();
        if (contentType != null) {
            return getExtensionFromMimeType(contentType);
        }
        return "";
    }

    private static String getExtensionFromMimeType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return MIME_TO_EXTENSION.getOrDefault(contentType.toLowerCase(), "");
    }

    public static String formatFileSize(long size) {
        if (size >= GB) {
            return String.format("%.2f GB", size / (double) GB);
        } else if (size >= MB) {
            return String.format("%.2f MB", size / (double) MB);
        } else if (size >= KB) {
            return String.format("%.2f KB", size / (double) KB);
        } else {
            return size + " B";
        }
    }

    public static boolean deleteFile(String filePath) {
        if (filePath == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }
}

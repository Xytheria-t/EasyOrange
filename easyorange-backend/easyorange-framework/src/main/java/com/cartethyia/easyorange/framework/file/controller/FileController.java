package com.cartethyia.easyorange.framework.file.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.service.FileService;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingService;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingService.ImageFormat;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingService.ProcessedImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final ImageProcessingService imageProcessingService;

    private static final long CACHE_MAX_AGE_SECONDS = TimeUnit.DAYS.toSeconds(365);
    private static final int DEFAULT_THUMBNAIL_SIZE = 200;
    private static final int[] PRESET_WIDTHS = {150, 300, 600, 1200};

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<UploadFileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessType", required = false) String businessType) {
        UploadFileVO result = fileService.uploadFile(file, businessType);
        return Result.success(result);
    }

    @PostMapping("/uploads")
    @PreAuthorize("isAuthenticated()")
    public Result<List<UploadFileVO>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "businessType", required = false) String businessType) {
        List<UploadFileVO> results = fileService.uploadFiles(files, businessType);
        return Result.success(results);
    }

    @GetMapping("/{id}")
    public Result<UploadFileVO> getFileInfo(@PathVariable Long id) {
        UploadFileVO file = fileService.getFileInfo(id);
        return Result.success(file);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return Result.success();
    }

    @GetMapping("/business/{businessType}/{businessId}")
    public Result<List<UploadFileVO>> getFilesByBusiness(
            @PathVariable String businessType,
            @PathVariable Long businessId) {
        List<UploadFileVO> files = fileService.getFilesByBusiness(businessType, businessId);
        return Result.success(files);
    }

    @PutMapping("/{id}/bind")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> bindBusiness(
            @PathVariable Long id,
            @RequestParam String businessType,
            @RequestParam Long businessId) {
        fileService.bindBusiness(id, businessType, businessId);
        return Result.success();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource resource = fileService.downloadFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewFile(
            @PathVariable Long id,
            @RequestParam(value = "w", required = false) Integer width,
            @RequestParam(value = "h", required = false) Integer height,
            @RequestParam(value = "format", required = false, defaultValue = "webp") String format,
            @RequestParam(value = "q", required = false, defaultValue = "80") Integer quality) throws IOException {

        Resource originalResource = fileService.downloadFile(id);
        File originalFile = originalResource.getFile();

        String mimeType = "application/octet-stream";
        Resource responseResource = originalResource;

        if (imageProcessingService.isImage(getMimeType(originalFile))) {
            ImageFormat imageFormat = parseFormat(format);
            float qualityValue = Math.max(10, Math.min(100, quality)) / 100f;

            if (width != null || height != null) {
                int targetWidth = width != null ? width : 0;
                int targetHeight = height != null ? height : 0;

                if (targetWidth == 0 && targetHeight > 0) {
                    targetWidth = targetHeight;
                } else if (targetHeight == 0 && targetWidth > 0) {
                    targetHeight = targetWidth;
                }

                ProcessedImage processed = imageProcessingService.processImage(
                        originalFile, targetWidth, targetHeight, imageFormat, qualityValue);
                responseResource = new FileSystemResource(processed.file());
                mimeType = processed.mimeType();
            } else if (imageProcessingService.supportsFormat(imageFormat)) {
                BufferedImage img = ImageIO.read(originalFile);
                ProcessedImage processed = imageProcessingService.processImage(
                        originalFile, img.getWidth(), img.getHeight(), imageFormat, qualityValue);
                responseResource = new FileSystemResource(processed.file());
                mimeType = processed.mimeType();
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE_SECONDS)
                .header(HttpHeaders.EXPIRES, String.valueOf(System.currentTimeMillis() + CACHE_MAX_AGE_SECONDS * 1000))
                .body(responseResource);
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(
            @PathVariable Long id,
            @RequestParam(value = "size", required = false, defaultValue = "200") Integer size) throws IOException {

        Resource originalResource = fileService.downloadFile(id);
        File originalFile = originalResource.getFile();

        if (!imageProcessingService.isImage(getMimeType(originalFile))) {
            return ResponseEntity.badRequest().build();
        }

        ProcessedImage thumbnail = imageProcessingService.createThumbnail(originalFile, size);
        Resource thumbnailResource = new FileSystemResource(thumbnail.file());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(thumbnail.mimeType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE_SECONDS)
                .header(HttpHeaders.EXPIRES, String.valueOf(System.currentTimeMillis() + CACHE_MAX_AGE_SECONDS * 1000))
                .body(thumbnailResource);
    }

    @GetMapping("/{id}/responsive")
    public ResponseEntity<Resource> getResponsiveImage(
            @PathVariable Long id,
            @RequestParam(value = "w", required = false) Integer width,
            @RequestParam(value = "format", required = false, defaultValue = "webp") String format,
            @RequestParam(value = "q", required = false, defaultValue = "80") Integer quality) throws IOException {

        Resource originalResource = fileService.downloadFile(id);
        File originalFile = originalResource.getFile();

        if (!imageProcessingService.isImage(getMimeType(originalFile))) {
            return ResponseEntity.badRequest().build();
        }

        int targetWidth = findClosestPresetWidth(width);
        ImageFormat imageFormat = parseFormat(format);
        float qualityValue = Math.max(10, Math.min(100, quality)) / 100f;

        ProcessedImage processed = imageProcessingService.processImage(
                originalFile, targetWidth, targetWidth, imageFormat, qualityValue);
        Resource responseResource = new FileSystemResource(processed.file());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(processed.mimeType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE_SECONDS)
                .header(HttpHeaders.EXPIRES, String.valueOf(System.currentTimeMillis() + CACHE_MAX_AGE_SECONDS * 1000))
                .body(responseResource);
    }

    private ImageFormat parseFormat(String format) {
        return switch (format.toLowerCase()) {
            case "webp" -> ImageFormat.WEBP;
            case "avif" -> ImageFormat.AVIF;
            case "png" -> ImageFormat.PNG;
            default -> ImageFormat.JPEG;
        };
    }

    private int findClosestPresetWidth(Integer width) {
        if (width == null) {
            return PRESET_WIDTHS[0];
        }
        int closest = PRESET_WIDTHS[0];
        int minDiff = Math.abs(width - closest);
        for (int preset : PRESET_WIDTHS) {
            int diff = Math.abs(width - preset);
            if (diff < minDiff) {
                minDiff = diff;
                closest = preset;
            }
        }
        return closest;
    }

    private String getMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".png")) {
            return "image/png";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        } else if (name.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}

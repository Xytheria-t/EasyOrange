package com.cartethyia.easyorange.framework.file.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.service.FileService;
import com.cartethyia.easyorange.framework.file.service.ImageProcessingService;
import com.cartethyia.easyorange.framework.file.service.ImageQueryService;
import com.cartethyia.easyorange.framework.file.service.ImageQueryService.ImageQueryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "文件服务", description = "图片上传/处理")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final ImageQueryService imageQueryService;

    private static final long CACHE_MAX_AGE_SECONDS = 31536000L; // 365 days

    @PostMapping("/upload")
    @Operation(summary = "单文件上传（需登录）：校验扩展名、大小与文件头后落本地存储，归属当前用户")
    public Result<UploadFileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessType", required = false) String businessType) {
        return Result.success(fileService.uploadFile(file, businessType));
    }

    @PostMapping("/uploads")
    @Operation(summary = "批量上传（跳过空文件），记录写入单事务；任一文件失败整体回滚")
    public Result<List<UploadFileVO>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "businessType", required = false) String businessType) {
        return Result.success(fileService.uploadFiles(files, businessType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "文件元信息（文件名 / URL / 大小 / MIME），不含文件内容")
    public Result<UploadFileVO> getFileInfo(@PathVariable String id) {
        return Result.success(fileService.getFileInfo(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件：仅上传者可删；存储层删除失败只告警仍删记录，并清该文件的图片加工缓存")
    public Result<Void> deleteFile(@PathVariable String id) {
        fileService.deleteFile(id);
        imageQueryService.evictCache(id);
        return Result.success();
    }

    @GetMapping("/business/{businessType}/{businessId}")
    @Operation(summary = "按业务类型 + 业务 ID 列出正常状态的文件，按上传时间升序")
    public Result<List<UploadFileVO>> getFilesByBusiness(
            @PathVariable String businessType, @PathVariable String businessId) {
        return Result.success(fileService.getFilesByBusiness(businessType, businessId));
    }

    @PutMapping("/{id}/bind")
    @Operation(summary = "把文件重新绑定到业务对象（businessType + businessId），覆盖原归属")
    public Result<Void> bindBusiness(
            @PathVariable String id, @RequestParam String businessType, @RequestParam String businessId) {
        fileService.bindBusiness(id, businessType, businessId);
        return Result.success();
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载原文件（attachment + octet-stream），不走图片加工链路")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) {
        Resource resource = fileService.downloadFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/view")
    @Operation(summary = "查看文件：非图片直出原文件；图片带 w/h 则缩放、否则按 format 转码，ETag 命中返 304")
    public ResponseEntity<Resource> getFileContent(
            @PathVariable String id,
            @RequestParam(value = "w", required = false) Integer width,
            @RequestParam(value = "h", required = false) Integer height,
            @RequestParam(value = "format", required = false, defaultValue = "webp") String format,
            @RequestParam(value = "q", required = false, defaultValue = "80") Integer quality,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch)
            throws IOException {

        return respond(imageQueryService.getForView(
                id, width, height, parseFormat(format), clampQuality(quality), ifNoneMatch));
    }

    @GetMapping("/{id}/thumbnail")
    @Operation(summary = "方形缩略图（固定 WEBP，size 默认 200）；非图片返回 400，命中 If-None-Match 返 304")
    public ResponseEntity<Resource> getThumbnail(
            @PathVariable String id,
            @RequestParam(value = "size", required = false, defaultValue = "200") Integer size,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch)
            throws IOException {

        return respond(imageQueryService.getThumbnail(id, size, ifNoneMatch));
    }

    @GetMapping("/{id}/responsive")
    @Operation(summary = "响应式方形图：请求宽度吸附到最近预设档 150/300/600/1200；非图片返回 400")
    public ResponseEntity<Resource> getResponsiveImage(
            @PathVariable String id,
            @RequestParam(value = "w", required = false) Integer width,
            @RequestParam(value = "format", required = false, defaultValue = "webp") String format,
            @RequestParam(value = "q", required = false, defaultValue = "80") Integer quality,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch)
            throws IOException {

        return respond(
                imageQueryService.getResponsive(id, width, parseFormat(format), clampQuality(quality), ifNoneMatch));
    }

    // ── 内部实现 ──

    private static float clampQuality(Integer quality) {
        if (quality == null) return 0.8f;
        return Math.clamp(quality, 10, 100) / 100f;
    }

    private static ImageProcessingService.ImageFormat parseFormat(String format) {
        return switch (format.toLowerCase()) {
            case "webp" -> ImageProcessingService.ImageFormat.WEBP;
            case "avif" -> ImageProcessingService.ImageFormat.AVIF;
            case "png" -> ImageProcessingService.ImageFormat.PNG;
            default -> ImageProcessingService.ImageFormat.JPEG;
        };
    }

    private ResponseEntity<Resource> respond(ImageQueryResult result) {
        if (result.resource() == null) return ResponseEntity.badRequest().build();
        if (result.notModified())
            return ResponseEntity.status(304)
                    .eTag(result.eTag())
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE_SECONDS)
                    .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .eTag(result.eTag())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE_SECONDS)
                .body(result.resource());
    }
}

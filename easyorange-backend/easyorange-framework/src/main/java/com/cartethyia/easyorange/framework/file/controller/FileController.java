package com.cartethyia.easyorange.framework.file.controller;

import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

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
}

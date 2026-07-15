package com.cartethyia.easyorange.framework.file.service.impl;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.exception.file.FileException;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.entity.UploadFile;
import com.cartethyia.easyorange.framework.file.mapper.UploadFileMapper;
import com.cartethyia.easyorange.framework.file.service.FileService;
import com.cartethyia.easyorange.framework.file.storage.FileStorage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.framework.util.FileUtils;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final UploadFileMapper uploadFileMapper;
    private final FileStorage fileStorage;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadFileVO uploadFile(MultipartFile file, String businessType) {
        return uploadFile(file, businessType, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadFileVO uploadFile(MultipartFile file, String businessType, String businessId) {
        Objects.requireNonNull(file, "上传文件不能为空");
        if (file.isEmpty()) throw BusinessException.of("上传文件不能为空");

        var userId = SecurityContextUtil.getCurrentUserId()
                .orElseThrow(() -> BusinessException.of("用户未登录"));

        try {
            FileUtils.assertAllowed(file, FileUtils.DEFAULT_ALLOWED_EXTENSION);
            var content = file.getBytes();
            var storageKey = fileStorage.store(content, file.getOriginalFilename(), file.getContentType());

            var entity = new UploadFile();
            entity.setFileName(file.getOriginalFilename());
            entity.setStorageKey(storageKey);
            entity.setFileUrl(fileStorage.getUrl(storageKey));
            entity.setFileSize(file.getSize());
            entity.setFileType(FileUtils.getExtension(file));
            entity.setMimeType(file.getContentType());
            entity.setStorageType("LOCAL");
            entity.setBusinessType(businessType);
            entity.setBusinessId(businessId);
            entity.setUploaderId(userId);
            entity.setStatus(CommonConstant.FILE_STATUS_NORMAL);

            uploadFileMapper.insert(entity);

            log.info("action=file_upload, filename={}, size={}", file.getOriginalFilename(), FileUtils.formatFileSize(file.getSize()));
            return toVo(entity);
        } catch (IOException e) {
            throw FileException.of("文件上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<UploadFileVO> uploadFiles(List<MultipartFile> files, String businessType) {
        Objects.requireNonNull(files, "上传的文件列表不能为空");
        return files.stream()
                .filter(f -> !f.isEmpty())
                .map(f -> uploadFile(f, businessType))
                .toList();
    }

    @Override
    public UploadFileVO getFileInfo(String fileId) {
        var entity = uploadFileMapper.selectById(fileId);
        if (entity == null) throw BusinessException.of("文件不存在");
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(String fileId) {
        var entity = uploadFileMapper.selectById(fileId);
        if (entity == null) throw BusinessException.of("文件不存在");

        var userId = SecurityContextUtil.getCurrentUserId()
                .orElseThrow(() -> BusinessException.of("用户未登录"));

        if (!Objects.equals(entity.getUploaderId(), userId)) {
            throw BusinessException.of("无权限删除该文件");
        }

        try {
            fileStorage.delete(entity.getStorageKey());
        } catch (IOException e) {
            log.warn("文件删除失败（存储层）：fileId={}", fileId, e);
        }
        uploadFileMapper.deleteById(fileId);
        log.info("action=file_delete, filename={}", entity.getFileName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindBusiness(String fileId, String businessType, String businessId) {
        var entity = uploadFileMapper.selectById(fileId);
        if (entity == null) throw BusinessException.of("文件不存在");
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessId);
        uploadFileMapper.updateById(entity);
    }

    @Override
    public List<UploadFileVO> getFilesByBusiness(String businessType, String businessId) {
        return uploadFileMapper.selectList(
                new LambdaQueryWrapper<UploadFile>()
                        .eq(UploadFile::getBusinessType, businessType)
                        .eq(UploadFile::getBusinessId, businessId)
                        .eq(UploadFile::getStatus, CommonConstant.FILE_STATUS_NORMAL)
                        .orderByAsc(UploadFile::getCreateTime))
                .stream()
                .map(FileServiceImpl::toVo)
                .toList();
    }

    @Override
    public Resource downloadFile(String fileId) {
        var entity = uploadFileMapper.selectById(fileId);
        if (entity == null) throw BusinessException.of("文件不存在");

        var path = fileStorage.getPath(entity.getStorageKey());
        if (!path.toFile().exists()) {
            log.error("文件不存在：fileId={}", fileId);
            throw FileException.of("文件不存在：" + entity.getFileName());
        }
        log.info("action=prepare_download, fileId={}, fileName={}", fileId, entity.getFileName());
        return new FileSystemResource(path);
    }

    private static UploadFileVO toVo(UploadFile entity) {
        return new UploadFileVO(
                entity.getId(),
                entity.getFileName(),
                entity.getFileUrl(),
                entity.getFileSize(),
                entity.getMimeType());
    }
}

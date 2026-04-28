package com.cartethyia.easyorange.framework.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.exception.FileException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.FileUtils;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.entity.UploadFile;
import com.cartethyia.easyorange.framework.file.mapper.UploadFileMapper;
import com.cartethyia.easyorange.framework.file.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<UploadFileMapper, UploadFile> implements FileService {

    @Value("${file.upload.path:./upload}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/api/file/}")
    private String urlPrefix;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadFileVO uploadFile(MultipartFile file, String businessType) {
        return uploadFile(file, businessType, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadFileVO uploadFile(MultipartFile file, String businessType, Long businessId) {
        BizRequire.notNull(file, "上传文件不能为空");
        BizRequire.isTrue(!file.isEmpty(), "上传文件不能为空");

        Long userId = SecurityContextUtil.getCurrentUserId()
                .orElseThrow(() -> BusinessException.of("用户未登录"));

        try {
            String relativePath = FileUtils.upload(uploadPath, file);
            String fileUrl = urlPrefix + relativePath.replace("\\", "/");
            String md5 = FileUtils.calculateMd5(file);

            UploadFile uploadFile = UploadFile.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(relativePath)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .fileType(FileUtils.getExtension(file))
                    .mimeType(file.getContentType())
                    .md5(md5)
                    .businessType(businessType)
                    .businessId(businessId)
                    .uploaderId(userId)
                    .status(CommonConstant.FILE_STATUS_NORMAL)
                    .build();

            save(uploadFile);

            log.info("action=file_upload, filename={}, size={}", file.getOriginalFilename(), FileUtils.formatFileSize(file.getSize()));
            return convertToVO(uploadFile);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e.getMessage());
            throw new FileException("文件上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<UploadFileVO> uploadFiles(List<MultipartFile> files, String businessType) {
        BizRequire.notEmpty(files, "上传的文件列表不能为空");
        BizRequire.noNullElements(files, "文件列表不能包含空元素");
        BizRequire.notBlank(businessType, "业务类型不能为空");
        
        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> uploadFile(file, businessType))
                .collect(Collectors.toList());
    }

    @Override
    public UploadFileVO getFileInfo(Long fileId) {
        UploadFile file = getById(fileId);
        BizRequire.notNull(file, "文件不存在");
        return convertToVO(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId) {
        UploadFile file = getById(fileId);
        BizRequire.notNull(file, "文件不存在");

        Long userId = SecurityContextUtil.getCurrentUserId()
                .orElseThrow(() -> BusinessException.of("用户未登录"));

        BizRequire.eq(file.getUploaderId(), userId, "无权限删除该文件");

        String fullPath = uploadPath + File.separator + file.getFilePath();
        if (!FileUtils.deleteFile(fullPath)) {
            throw new FileException("文件删除失败：" + file.getFileName());
        }

        removeById(fileId);
        log.info("action=file_delete, filename={}", file.getFileName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindBusiness(Long fileId, String businessType, Long businessId) {
        UploadFile file = getById(fileId);
        BizRequire.notNull(file, "文件不存在");

        file.setBusinessType(businessType);
        file.setBusinessId(businessId);
        updateById(file);
    }

    @Override
    public List<UploadFileVO> getFilesByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<UploadFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UploadFile::getBusinessType, businessType)
                .eq(UploadFile::getBusinessId, businessId)
                .eq(UploadFile::getStatus, CommonConstant.FILE_STATUS_NORMAL)
                .orderByAsc(UploadFile::getCreateTime);

        List<UploadFile> files = list(wrapper);
        return files.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Resource downloadFile(Long fileId) {
        UploadFile file = getById(fileId);
        BizRequire.notNull(file, "文件不存在");

        String fullPath = uploadPath + File.separator + file.getFilePath();

        if (!FileUtils.exists(fullPath)) {
            log.error("文件不存在：fileId={}, fullPath={}", fileId, fullPath);
            throw new FileException("文件不存在：" + file.getFileName());
        }

        String fileName = FileUtils.getFileName(file.getFilePath());
        log.info("action=prepare_download, fileId={}, fileName={}", fileId, fileName);

        return new FileSystemResource(fullPath);
    }

    private UploadFileVO convertToVO(UploadFile file) {
        return UploadFileVO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .fileType(file.getFileType())
                .mimeType(file.getMimeType())
                .md5(file.getMd5())
                .build();
    }
}

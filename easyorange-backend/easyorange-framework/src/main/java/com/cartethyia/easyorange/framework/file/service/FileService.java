package com.cartethyia.easyorange.framework.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.entity.UploadFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService extends IService<UploadFile> {

    UploadFileVO uploadFile(MultipartFile file, String businessType);

    UploadFileVO uploadFile(MultipartFile file, String businessType, Long businessId);

    List<UploadFileVO> uploadFiles(List<MultipartFile> files, String businessType);

    UploadFileVO getFileInfo(Long fileId);

    void deleteFile(Long fileId);

    void bindBusiness(Long fileId, String businessType, Long businessId);

    List<UploadFileVO> getFilesByBusiness(String businessType, Long businessId);

    Resource downloadFile(Long fileId);
}

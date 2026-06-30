package com.cartethyia.easyorange.framework.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.framework.file.dto.UploadFileVO;
import com.cartethyia.easyorange.framework.file.entity.UploadFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService extends IService<UploadFile> {

    UploadFileVO uploadFile(MultipartFile file, String businessType);

    UploadFileVO uploadFile(MultipartFile file, String businessType, String businessId);

    List<UploadFileVO> uploadFiles(List<MultipartFile> files, String businessType);

    UploadFileVO getFileInfo(String fileId);

    void deleteFile(String fileId);

    void bindBusiness(String fileId, String businessType, String businessId);

    List<UploadFileVO> getFilesByBusiness(String businessType, String businessId);

    Resource downloadFile(String fileId);
}

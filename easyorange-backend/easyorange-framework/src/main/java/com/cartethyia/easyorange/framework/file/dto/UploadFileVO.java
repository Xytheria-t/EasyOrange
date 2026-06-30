package com.cartethyia.easyorange.framework.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UploadFileVO {

    private String id;

    private String fileName;

    private String filePath;

    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private String mimeType;

    private String md5;

    private String storageType;

    private String storageKey;
}

package com.cartethyia.easyorange.framework.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_upload_file")
public class UploadFile extends BaseDO {

    private String fileName;

    private String filePath;

    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private String mimeType;

    private String md5;

    private String storageType;

    private String storageKey;

    private String businessType;

    private Long businessId;

    private Long uploaderId;

    private Integer status;
}

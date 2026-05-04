package com.cartethyia.easyorange.framework.operlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("eo_oper_log")
public class SysOperLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long operId;

    private String title;

    private Integer businessType;

    private String method;

    private String requestMethod;

    private Integer operatorType;

    private String operName;

    private String operIp;

    private String operUrl;

    private String operLocation;

    private String operParam;

    private String jsonResult;

    private Integer status;

    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operTime;

    private Integer costTime;
}

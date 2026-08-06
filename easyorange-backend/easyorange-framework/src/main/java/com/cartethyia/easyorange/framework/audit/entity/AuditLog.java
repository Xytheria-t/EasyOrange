package com.cartethyia.easyorange.framework.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审计日志实体
 * <p>
 * 记录所有写操作（新增/修改/删除等）的审计轨迹：操作人、IP、请求参数、响应、耗时等。
 * 通过 {@link com.cartethyia.easyorange.framework.audit.aspect.AuditLogAspect} 自动拦截记录。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("eo_audit_log")
public class AuditLog {

    @TableId(type = IdType.INPUT)
    private String id;

    /** 模块标题，如 "商品管理-创建" */
    private String title;

    /** 业务类型编码（对应 {@link com.cartethyia.easyorange.common.enums.BusinessType}） */
    private String businessType;

    /** 完整方法签名 */
    private String method;

    /** HTTP 请求方式（GET/POST/PUT/DELETE） */
    private String requestMethod;

    /** 操作类别（1=管理员, 2=普通用户, 0=未登录） */
    private Integer operatorType;

    /** 操作人员用户名 */
    private String username;

    /** 客户端 IP */
    private String clientIp;

    /** 请求 URL */
    private String requestUrl;

    /** 操作地点（预留） */
    private String operLocation;

    /** 请求参数（JSON 字符串，敏感字段已掩码） */
    private String requestParams;

    /** 响应数据（JSON 字符串） */
    private String responseData;

    /** 操作状态（0 正常, 1 异常） */
    private Integer status;

    /** 错误消息 */
    private String errorMsg;

    /** 操作时间 */
    @JsonFormat(pattern = CommonConstant.DATETIME_FORMAT)
    private LocalDateTime createdAt;

    /** 执行耗时（毫秒） */
    private Integer duration;
}

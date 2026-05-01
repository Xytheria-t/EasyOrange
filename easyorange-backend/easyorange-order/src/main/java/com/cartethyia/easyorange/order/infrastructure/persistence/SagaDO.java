package com.cartethyia.easyorange.order.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("saga_status")
public class SagaDO {
    @TableId(type = IdType.ASSIGN_UUID)
    private String sagaId;
    private String sagaType;
    private String state;
    private String currentStep;
    private String payload;
    private String errorMessage;
    private String compensationLog;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

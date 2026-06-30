package com.cartethyia.easyorange.payment.adapter.outbound.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_idempotency_key")
public class IdempotencyKeyPO extends BaseDO {

    private String idempotencyKey;

    private String userId;

    private String requestHash;

    private String responseData;

    private String status;

    private LocalDateTime expiresAt;
}

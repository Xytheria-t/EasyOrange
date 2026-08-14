package com.cartethyia.easyorange.ai.adapter.outbound.persistence.preference;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eo_user_preference")
public class UserPreferenceDO extends BaseDO {

    private String userId;
    private String prefKey;
    private String prefValue;
}

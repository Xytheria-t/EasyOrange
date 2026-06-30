package com.cartethyia.easyorange.favorite.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@TableName("eo_favorite")
@SuperBuilder(toBuilder = true)
@Getter
@Setter
public class FavoriteDO extends BaseDO {

    private String userId;
    private String productId;

    public FavoriteDO() {}

    public FavoriteDO(String userId, String productId) {
        this.userId = userId;
        this.productId = productId;
    }
}

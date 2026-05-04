package com.cartethyia.easyorange.favorite.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@TableName("eo_favorite")
@SuperBuilder(toBuilder = true)
@Getter
@Setter
public class FavoriteDO extends BaseDO {

    private Long userId;
    private Long productId;

    public FavoriteDO() {}

    public FavoriteDO(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }
}

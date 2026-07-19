package com.cartethyia.easyorange.product.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import java.time.LocalDateTime;
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
@TableName("eo_product_review")
public class ProductRatingDO extends BaseDO {

    private String productId;

    private String userId;

    private String orderId;

    private Integer rating;

    private String content;

    private String replyContent;

    private LocalDateTime replyTime;

    private Integer likes;

    private Integer status;
}

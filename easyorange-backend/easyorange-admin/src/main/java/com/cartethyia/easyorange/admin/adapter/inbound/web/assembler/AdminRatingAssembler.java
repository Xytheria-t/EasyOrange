package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminRatingResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminRatingQueryPort.RatingSummary;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserInfo;
import org.springframework.stereotype.Component;

@Component
public class AdminRatingAssembler {

    public AdminRatingResponse toAdminRatingResponse(RatingSummary review, UserInfo user, ProductInfo product) {
        return AdminRatingResponse.builder()
            .reviewId(review.id())
            .productId(review.productId())
            .productName(product != null ? product.name() : null)
            .userId(review.userId())
            .username(resolveUsername(user))
            .userAvatar(user != null ? user.avatar() : null)
            .rating(review.rating())
            .content(review.content())
            .replyContent(review.replyContent())
            .likes(review.likes())
            .status(review.status())
            .createTime(review.createTime())
            .updateTime(review.updateTime())
            .build();
    }

    private String resolveUsername(UserInfo user) {
        if (user == null) {
            return null;
        }
        return user.nickName() != null ? user.nickName() : user.username();
    }
}

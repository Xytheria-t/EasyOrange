package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingDeleteRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminRatingQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminRatingResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductRatingDO;
import com.cartethyia.easyorange.admin.util.BatchQueryUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductRatingMapper;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRatingService {

    private final ProductRatingMapper reviewMapper;
    private final BatchQueryUtil batchQueryUtil;

    @Transactional(readOnly = true)
    public PageResult<AdminRatingResponse> listReviews(AdminRatingQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        Page<ProductRatingDO> page = new Page<>(pageNum, pageSize);
        var wrapper = buildQueryWrapper(request);

        Page<ProductRatingDO> reviewPage = wrapper.page(page);

        if (reviewPage.getRecords().isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        Map<String, ProductDO> productMap = batchQueryUtil.batchGetProducts(reviewPage.getRecords().stream().map(ProductRatingDO::getProductId).distinct().toList());
        Map<String, UserEntity> userMap = batchQueryUtil.batchGetUsers(reviewPage.getRecords().stream().map(ProductRatingDO::getUserId).distinct().toList());

        List<AdminRatingResponse> records = reviewPage.getRecords().stream()
            .map(r -> toAdminRatingResponse(r, productMap, userMap))
            .collect(Collectors.toList());

        return PageResult.of(records, reviewPage.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public AdminRatingResponse getReviewDetail(String id) {
        ProductRatingDO review = reviewMapper.selectById(id);
        if (review == null || review.getDelFlag() != 0) {
            throw BusinessException.of("评价不存在");
        }

        Map<String, ProductDO> productMap = batchQueryUtil.batchGetProducts(List.of(review.getProductId()));
        Map<String, UserEntity> userMap = batchQueryUtil.batchGetUsers(List.of(review.getUserId()));

        return toAdminRatingResponse(review, productMap, userMap);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(String id, AdminRatingDeleteRequest request) {
        ProductRatingDO review = reviewMapper.selectById(id);
        if (review == null || review.getDelFlag() != 0) {
            throw BusinessException.of("评价不存在或已被删除");
        }

        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        reviewMapper.deleteById(id);

        log.info("action=admin_delete_review reviewId={} operatorId={} reason={}",
            id, operatorId, request.getReason());
    }

    private LambdaQueryChainWrapper<ProductRatingDO> buildQueryWrapper(AdminRatingQueryRequest request) {
        var wrapper = ChainWrappers.lambdaQueryChain(reviewMapper)
            .eq(ProductRatingDO::getDelFlag, 0);

        if (request.getProductId() != null) {
            wrapper.eq(ProductRatingDO::getProductId, request.getProductId());
        }
        if (request.getUserId() != null) {
            wrapper.eq(ProductRatingDO::getUserId, request.getUserId());
        }
        if (request.getRating() != null) {
            wrapper.eq(ProductRatingDO::getRating, request.getRating());
        }
        if (request.getStatus() != null) {
            wrapper.eq(ProductRatingDO::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(ProductRatingDO::getContent, request.getKeyword());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(ProductRatingDO::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(ProductRatingDO::getCreateTime, request.getEndTime());
        }

        wrapper.orderByDesc(ProductRatingDO::getCreateTime);
        return wrapper;
    }

    private AdminRatingResponse toAdminRatingResponse(
        ProductRatingDO review,
        Map<String, ProductDO> productMap,
        Map<String, UserEntity> userMap
    ) {
        ProductDO product = productMap.get(review.getProductId());
        UserEntity user = userMap.get(review.getUserId());

        return AdminRatingResponse.builder()
            .reviewId(review.getId())
            .productId(review.getProductId())
            .productName(product != null ? product.getName() : null)
            .userId(review.getUserId())
            .username(resolveUsername(user))
            .userAvatar(user != null ? user.getAvatar() : null)
            .rating(review.getRating())
            .content(review.getContent())
            .replyContent(review.getReplyContent())
            .likes(review.getLikes())
            .status(review.getStatus())
            .createTime(review.getCreateTime())
            .updateTime(review.getUpdateTime())
            .build();
    }

    private String resolveUsername(UserEntity user) {
        if (user == null) {
            return null;
        }
        return user.getNickName() != null ? user.getNickName() : user.getUsername();
    }

}

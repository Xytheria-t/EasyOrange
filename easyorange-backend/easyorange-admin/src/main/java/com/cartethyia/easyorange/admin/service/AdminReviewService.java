package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.admin.dto.request.AdminReviewDeleteRequest;
import com.cartethyia.easyorange.admin.dto.request.AdminReviewQueryRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminReviewVO;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReviewDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReviewMapper;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ProductReviewMapper reviewMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResult<AdminReviewVO> listReviews(AdminReviewQueryRequest request) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        Page<ProductReviewDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductReviewDO> wrapper = buildQueryWrapper(request);

        Page<ProductReviewDO> reviewPage = reviewMapper.selectPage(page, wrapper);

        if (reviewPage.getRecords().isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        Map<Long, ProductDO> productMap = batchGetProducts(reviewPage.getRecords());
        Map<Long, UserEntity> userMap = batchGetUsers(reviewPage.getRecords());

        List<AdminReviewVO> records = reviewPage.getRecords().stream()
            .map(r -> toAdminReviewVO(r, productMap, userMap))
            .collect(Collectors.toList());

        return PageResult.of(records, reviewPage.getTotal(), pageNum, pageSize);
    }

    @Transactional(readOnly = true)
    public AdminReviewVO getReviewDetail(Long id) {
        ProductReviewDO review = reviewMapper.selectById(id);
        if (review == null || review.getDelFlag() != 0) {
            throw BusinessException.of("评价不存在");
        }

        Map<Long, ProductDO> productMap = batchGetProducts(List.of(review));
        Map<Long, UserEntity> userMap = batchGetUsers(List.of(review));

        return toAdminReviewVO(review, productMap, userMap);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long id, AdminReviewDeleteRequest request) {
        ProductReviewDO review = reviewMapper.selectById(id);
        if (review == null || review.getDelFlag() != 0) {
            throw BusinessException.of("评价不存在或已被删除");
        }

        Long operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        reviewMapper.deleteById(id);

        log.info("action=admin_delete_review reviewId={} operatorId={} reason={}",
            id, operatorId, request.getReason());
    }

    private LambdaQueryWrapper<ProductReviewDO> buildQueryWrapper(AdminReviewQueryRequest request) {
        LambdaQueryWrapper<ProductReviewDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReviewDO::getDelFlag, 0);

        if (request.getProductId() != null) {
            wrapper.eq(ProductReviewDO::getProductId, request.getProductId());
        }
        if (request.getUserId() != null) {
            wrapper.eq(ProductReviewDO::getUserId, request.getUserId());
        }
        if (request.getRating() != null) {
            wrapper.eq(ProductReviewDO::getRating, request.getRating());
        }
        if (request.getStatus() != null) {
            wrapper.eq(ProductReviewDO::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(ProductReviewDO::getContent, request.getKeyword());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(ProductReviewDO::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(ProductReviewDO::getCreateTime, request.getEndTime());
        }

        wrapper.orderByDesc(ProductReviewDO::getCreateTime);
        return wrapper;
    }

    private AdminReviewVO toAdminReviewVO(
        ProductReviewDO review,
        Map<Long, ProductDO> productMap,
        Map<Long, UserEntity> userMap
    ) {
        ProductDO product = productMap.get(review.getProductId());
        UserEntity user = userMap.get(review.getUserId());

        return AdminReviewVO.builder()
            .reviewId(review.getId())
            .productId(review.getProductId())
            .productName(product != null ? product.getName() : null)
            .userId(review.getUserId())
            .username(user != null ? user.getNickName() != null ? user.getNickName() : user.getUsername() : null)
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

    private Map<Long, ProductDO> batchGetProducts(List<ProductReviewDO> reviews) {
        Set<Long> productIds = reviews.stream()
            .map(ProductReviewDO::getProductId)
            .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductDO> products = productMapper.selectBatchIds(productIds);
        return products.stream().collect(Collectors.toMap(ProductDO::getId, p -> p, (a, b) -> a));
    }

    private Map<Long, UserEntity> batchGetUsers(List<ProductReviewDO> reviews) {
        Set<Long> userIds = reviews.stream()
            .map(ProductReviewDO::getUserId)
            .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(UserEntity::getId, u -> u, (a, b) -> a));
    }
}

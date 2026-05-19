package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.admin.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductAuditLogDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductAuditLogMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.cartethyia.easyorange.product.application.query.dto.SellerInfo;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductAuditService {

    private final ProductMapper productMapper;
    private final ProductAuditLogMapper productAuditLogMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final AiReviewService aiReviewService;

    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(Long id, ProductAuditRequest request) {
        ProductDO product = productMapper.selectById(id);
        if (product == null || product.getDelFlag() != 0) {
            throw BusinessException.of("商品不存在");
        }

        if (product.getStatus() != ProductStatus.PENDING_REVIEW.getCode()) {
            throw BusinessException.of("只有待审核状态的商品可以审核");
        }

        Long operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String operatorName = SecurityContextUtil.getUserContext()
                .map(authUser -> authUser.username())
                .orElse("管理员");

        Integer beforeStatus = product.getStatus();
        Integer action = request.getAction();

        if (action == 1) {
            product.setStatus(ProductStatus.ONLINE.getCode());
        } else if (action == 2) {
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw BusinessException.of("拒绝时必须填写原因");
            }
            product.setStatus(ProductStatus.REJECTED.getCode());
        } else {
            throw BusinessException.of("无效的审核动作");
        }

        Integer afterStatus = product.getStatus();
        productMapper.updateById(product);

        ProductAuditLogDO auditLog = ProductAuditLogDO.builder()
                .productId(id)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .action(action)
                .reason(request.getReason())
                .auditDimensions(toJsonString(request.getDimensions()))
                .beforeStatus(beforeStatus)
                .afterStatus(afterStatus)
                .remark(request.getRemark())
                .build();
        productAuditLogMapper.insert(auditLog);

        ProductAuditedEvent event = new ProductAuditedEvent(
                id,
                product.getName(),
                product.getUserId(),
                action,
                request.getReason(),
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);

        log.info("action=audit_product productId={} action={} operatorId={} beforeStatus={} afterStatus={}",
                id, action, operatorId, beforeStatus, afterStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchAuditResultResponse batchAudit(BatchAuditRequest request) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (BatchAuditRequest.AuditItem item : request.getItems()) {
            try {
                ProductDO product = productMapper.selectById(item.productId());
                if (product == null || product.getDelFlag() != 0) {
                    errors.add("商品ID " + item.productId() + ": 不存在");
                    continue;
                }

                if (product.getStatus() != ProductStatus.PENDING_REVIEW.getCode()) {
                    errors.add("商品ID " + item.productId() + ": 非待审核状态，无法操作");
                    continue;
                }

                Long operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
                String operatorName = SecurityContextUtil.getUserContext()
                        .map(authUser -> authUser.username())
                        .orElse("管理员");

                Integer beforeStatus = product.getStatus();
                Integer action = item.action();

                if (action == 1) {
                    product.setStatus(ProductStatus.ONLINE.getCode());
                } else if (action == 2) {
                    if (item.reason() == null || item.reason().isBlank()) {
                        errors.add("商品ID " + item.productId() + ": 拒绝时必须填写原因");
                        continue;
                    }
                    product.setStatus(ProductStatus.REJECTED.getCode());
                } else {
                    errors.add("商品ID " + item.productId() + ": 无效的审核动作 " + action);
                    continue;
                }

                Integer afterStatus = product.getStatus();
                productMapper.updateById(product);

                ProductAuditLogDO auditLog = ProductAuditLogDO.builder()
                        .productId(item.productId())
                        .operatorId(operatorId)
                        .operatorName(operatorName)
                        .action(action)
                        .reason(item.reason())
                        .auditDimensions(toJsonString(item.dimensions()))
                        .beforeStatus(beforeStatus)
                        .afterStatus(afterStatus)
                        .build();
                productAuditLogMapper.insert(auditLog);

                ProductAuditedEvent event = new ProductAuditedEvent(
                        item.productId(),
                        product.getName(),
                        product.getUserId(),
                        action,
                        item.reason(),
                        LocalDateTime.now()
                );
                eventPublisher.publishEvent(event);

                successCount++;
            } catch (BusinessException e) {
                errors.add("商品ID " + item.productId() + ": " + e.getMessage());
            } catch (Exception e) {
                errors.add("商品ID " + item.productId() + ": " + e.getMessage());
            }
        }

        return new BatchAuditResultResponse(
                request.getItems().size(),
                successCount,
                errors.size(),
                errors
        );
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(Long productId) {
        List<ProductAuditLogDO> logs = productAuditLogMapper.selectByProductId(productId);
        return logs.stream().map(this::toAuditLogResponse).toList();
    }

    @Transactional(readOnly = true)
    public AiReviewResult getAiReview(Long productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || product.getDelFlag() != 0) {
            throw BusinessException.of("商品不存在");
        }

        List<ProductDetailDO> details = productMapper.selectDetailsByProductIds(List.of(productId));
        String description = details.isEmpty() ? null : details.get(0).getDescription();

        List<CategoryDO> categories = productMapper.selectCategoriesByIds(List.of(product.getCategoryId()));
        String categoryName = categories.isEmpty() ? null : categories.get(0).getName();

        Set<Long> sellerIds = Set.of(product.getUserId());
        List<SellerInfo> sellers = productMapper.selectSellersByIds(sellerIds);
        String sellerName = sellers.isEmpty() ? null : sellers.get(0).nickName();

        List<ProductImageDO> images = productMapper.selectImagesByProductIds(List.of(productId));
        List<String> imageUrls = images.stream()
                .map(ProductImageDO::getImageUrl)
                .collect(Collectors.toList());

        return aiReviewService.reviewProduct(
                product.getName(),
                description,
                categoryName,
                product.getConditionLevel(),
                product.getPrice().toString(),
                sellerName,
                imageUrls
        );
    }

    private AuditLogResponse toAuditLogResponse(ProductAuditLogDO log) {
        return new AuditLogResponse(
                log.getId(),
                log.getProductId(),
                log.getOperatorId(),
                log.getOperatorName(),
                log.getAction(),
                getActionDesc(log.getAction()),
                log.getReason(),
                parseDimensions(log.getAuditDimensions()),
                log.getBeforeStatus(),
                ProductStatus.getDescByCode(log.getBeforeStatus()),
                log.getAfterStatus(),
                ProductStatus.getDescByCode(log.getAfterStatus()),
                log.getRemark(),
                log.getCreateTime()
        );
    }

    private String getActionDesc(Integer action) {
        if (action == null) {
            return "未知";
        }
        return switch (action) {
            case 1 -> "通过";
            case 2 -> "拒绝";
            default -> "未知";
        };
    }

    private String toJsonString(List<String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit dimensions to JSON", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseDimensions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse audit dimensions from JSON: {}", json, e);
            return List.of();
        }
    }
}

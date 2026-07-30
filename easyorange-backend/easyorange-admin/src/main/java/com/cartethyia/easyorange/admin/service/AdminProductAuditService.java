package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ProductRepository productRepository;
    private final ProductAuditLogRepository productAuditLogRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;
    private final AiReviewService aiReviewService;

    // ProductMapper is kept only for getAiReview() which needs cross-table read queries.
    // The audit flow (auditProduct, batchAudit) uses ProductRepository + Product aggregate.
    private final ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(String id, ProductAuditRequest request) {
        Product product = productRepository.findById(ProductId.of(id))
                .orElseThrow(() -> BusinessException.of("商品不存在"));

        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String operatorName = SecurityContextUtil.getUserContext()
                .map(authUser -> authUser.username())
                .orElse("管理员");

        String beforeStatus = product.getStatus().getCode();
        AuditAction action = AuditAction.fromCode(String.valueOf(request.action()));
        if (action == null) {
            throw BusinessException.of("无效的审核动作");
        }

        Transition<Product, ?> t = switch (action) {
            case APPROVED -> product.approve(request.reason());
            case REJECTED -> {
                BizRequire.notBlank(request.reason(), "拒绝时必须填写原因");
                yield product.reject(request.reason());
            }
            default -> throw BusinessException.of("无效的审核动作");
        };

        productRepository.save(t.aggregate());

        ProductAuditLog auditLog = ProductAuditLog.builder()
                .productId(id)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .action(action.getCode())
                .reason(request.reason())
                .auditDimensions(toJsonString(request.dimensions()))
                .beforeStatus(beforeStatus)
                .afterStatus(product.getStatus().getCode())
                .remark(request.remark())
                .build();
        productAuditLogRepository.save(auditLog);

        domainEventPublisher.publish(t.event());

        log.info("action=audit_product productId={} action={} operatorId={} beforeStatus={} afterStatus={}",
                id, action.getCode(), operatorId, beforeStatus, product.getStatus().getCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchAuditResultResponse batchAudit(BatchAuditRequest request) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String operatorName = SecurityContextUtil.getUserContext()
                .map(authUser -> authUser.username())
                .orElse("管理员");

        for (BatchAuditRequest.AuditItem item : request.getItems()) {
            try {
                Product product = productRepository.findById(ProductId.of(item.productId()))
                        .orElse(null);
                if (product == null) {
                    errors.add("商品ID " + item.productId() + ": 不存在");
                    continue;
                }

                AuditAction action = AuditAction.fromCode(String.valueOf(item.action()));
                if (action == null) {
                    errors.add("商品ID " + item.productId() + ": 无效的审核动作 " + item.action());
                    continue;
                }

                String beforeStatus = product.getStatus().getCode();

                Transition<Product, ?> t = applyTransition(product, action, item, errors);
                if (t == null) continue;

productRepository.save(t.aggregate());

                ProductAuditLog auditLog = ProductAuditLog.builder()
                        .productId(item.productId())
                        .operatorId(operatorId)
                        .operatorName(operatorName)
                        .action(action.getCode())
                        .reason(item.reason())
                        .auditDimensions(toJsonString(item.dimensions()))
                        .beforeStatus(beforeStatus)
                        .afterStatus(product.getStatus().getCode())
                        .build();
                productAuditLogRepository.save(auditLog);

                domainEventPublisher.publish(t.event());

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
    public List<AuditLogResponse> getAuditLogs(String productId) {
        var logs = productAuditLogRepository.findByProductId(productId);
        return logs.stream().map(this::toAuditLogResponse).toList();
    }

    @Transactional(readOnly = true)
    public AiReviewResult getAiReview(String productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || product.getDelFlag() != 0) {
            throw BusinessException.of("商品不存在");
        }

        List<ProductDetailDO> details = productMapper.selectDetailsByProductIds(List.of(productId));
        String description = details.isEmpty() ? null : details.get(0).getDescription();

        List<CategoryDO> categories = productMapper.selectCategoriesByIds(List.of(product.getCategoryId()));
        String categoryName = categories.isEmpty() ? null : categories.get(0).getName();

        Set<String> sellerIds = Set.of(product.getUserId());
        List<SellerReadModel> sellers = productMapper.selectSellersByIds(sellerIds);
        String sellerName = sellers.isEmpty() ? null : sellers.get(0).nickName();

        List<ProductImageDO> images = productMapper.selectImagesByProductIds(List.of(productId));
        List<String> imageUrls = images.stream()
                .map(ProductImageDO::getImageUrl)
                .collect(Collectors.toList());

        return aiReviewService.reviewProduct(
                product.getName(),
                description,
                categoryName,
                product.getConditionLevel() != null ? product.getConditionLevel().getCode() : null,
                product.getPrice().toString(),
                sellerName,
                imageUrls
        );
    }

    private AuditLogResponse toAuditLogResponse(ProductAuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getProductId(),
                log.getOperatorId(),
                log.getOperatorName(),
                Integer.valueOf(log.getAction()),
                AuditAction.getDescByCode(log.getAction()),
                log.getReason(),
                parseDimensions(log.getAuditDimensions()),
                log.getBeforeStatus(),
                describeStatus(log.getBeforeStatus()),
                log.getAfterStatus(),
                describeStatus(log.getAfterStatus()),
                log.getRemark(),
                log.getCreateTime()
        );
    }

    private String describeStatus(String code) {
        if (code == null) return "未知状态";
        try {
            return ProductStatus.fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知状态";
        }
    }

    private String toJsonString(List<String> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (JacksonException e) {
            log.warn("Failed to serialize audit dimensions to JSON", e);
            return null;
        }
    }

    private static Transition<Product, ?> applyTransition(Product product, AuditAction action,
                                                           BatchAuditRequest.AuditItem item, List<String> errors) {
        return switch (action) {
            case APPROVED -> product.approve(item.reason());
            case REJECTED -> {
                if (item.reason() == null || item.reason().isBlank()) {
                    errors.add("商品ID " + item.productId() + ": 拒绝时必须填写原因");
                    yield null;
                }
                yield product.reject(item.reason());
            }
            default -> {
                errors.add("商品ID " + item.productId() + ": 无效的审核动作 " + item.action());
                yield null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> parseDimensions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JacksonException e) {
            log.warn("Failed to parse audit dimensions from JSON: {}", json, e);
            return List.of();
        }
    }
}

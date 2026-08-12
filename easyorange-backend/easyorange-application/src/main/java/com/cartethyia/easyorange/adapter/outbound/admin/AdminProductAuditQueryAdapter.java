package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.admin.domain.port.AdminProductAuditQueryPort;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Admin 商品审核适配器
 * <p>
 * 实现 {@link AdminProductAuditQueryPort}，执行商品审核、查询审核日志与 AI 预审。
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class AdminProductAuditQueryAdapter implements AdminProductAuditQueryPort {

    private static final TypeReference<List<String>> DIMENSIONS_TYPE = new TypeReference<>() {};

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductRepository productRepository;
    private final ProductAuditLogRepository productAuditLogRepository;
    private final AiReviewService aiReviewService;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public AiReviewData getAiReviewData(String productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || product.getDelFlag() != 0) {
            return null;
        }

        List<ProductDetailDO> details = productDetailMapper.selectDetailsByProductIds(List.of(productId));
        String description = details.isEmpty() ? null : details.get(0).getDescription();

        List<CategoryDO> categories = productMapper.selectCategoriesByIds(List.of(product.getCategoryId()));
        String categoryName = categories.isEmpty() ? null : categories.get(0).getName();

        List<SellerReadModel> sellers = productMapper.selectSellersByIds(Set.of(product.getUserId()));
        String sellerName = sellers.isEmpty() ? null : sellers.get(0).nickName();

        List<String> imageUrls = productMapper.selectImagesByProductIds(List.of(productId)).stream()
                .map(ProductImageDO::getImageUrl)
                .toList();

        return new AiReviewData(
                product.getName(),
                description,
                categoryName,
                product.getConditionLevel() != null
                        ? product.getConditionLevel().getCode()
                        : null,
                product.getPrice(),
                sellerName,
                imageUrls);
    }

    @Override
    public void auditProduct(
            String productId,
            Integer actionCode,
            String reason,
            String remark,
            List<String> dimensions,
            String operatorId,
            String operatorName) {
        Product product = productRepository
                .findById(ProductId.of(productId))
                .orElseThrow(() -> new ProductNotFoundException(productId));
        AuditAction action = parseAction(actionCode);

        String beforeStatus = product.getStatus().getCode();
        Transition<Product, ?> t =
                switch (action) {
                    case APPROVED -> product.approve(reason);
                    case REJECTED -> {
                        BizRequire.notBlank(reason, "拒绝时必须填写原因");
                        yield product.reject(reason);
                    }
                    default -> throw BusinessException.of("无效的审核动作");
                };

        productRepository.save(t.aggregate());

        productAuditLogRepository.save(ProductAuditLog.builder()
                .productId(product.getId().value())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .action(action.getCode())
                .reason(reason)
                .auditDimensions(toJsonString(dimensions))
                .beforeStatus(beforeStatus)
                .afterStatus(product.getStatus().getCode())
                .remark(remark)
                .build());

        domainEventPublisher.publish(t.event());

        log.info(
                "action=audit_product productId={} action={} operatorId={} beforeStatus={} afterStatus={}",
                product.getId().value(),
                action.getCode(),
                operatorId,
                beforeStatus,
                product.getStatus().getCode());
    }

    @Override
    public List<AuditLogRecord> getAuditLogs(String productId) {
        return productAuditLogRepository.findByProductId(productId).stream()
                .map(this::toAuditLogRecord)
                .toList();
    }

    @Override
    public AiReviewRecord getAiReview(String productId) {
        AiReviewData data = getAiReviewData(productId);
        if (data == null) {
            throw new ProductNotFoundException(ProductId.of(productId));
        }
        AiReviewResult result = aiReviewService.reviewProduct(
                data.name(),
                data.description(),
                data.categoryName(),
                data.conditionLevel(),
                data.price().toString(),
                data.sellerName(),
                data.imageUrls());
        return new AiReviewRecord(
                result.suggestedAction(),
                result.suggestedActionDesc(),
                result.confidenceScore(),
                result.riskFlags(),
                result.reasoning());
    }

    private static AuditAction parseAction(Integer actionCode) {
        try {
            return AuditAction.fromCode(String.valueOf(actionCode));
        } catch (IllegalArgumentException e) {
            throw BusinessException.of("无效的审核动作");
        }
    }

    private AuditLogRecord toAuditLogRecord(ProductAuditLog auditLog) {
        return new AuditLogRecord(
                auditLog.getId(),
                auditLog.getProductId(),
                auditLog.getOperatorId(),
                auditLog.getOperatorName(),
                auditLog.getAction(),
                AuditAction.getDescByCode(auditLog.getAction()),
                auditLog.getReason(),
                parseDimensions(auditLog.getAuditDimensions()),
                auditLog.getBeforeStatus(),
                describeStatus(auditLog.getBeforeStatus()),
                auditLog.getAfterStatus(),
                describeStatus(auditLog.getAfterStatus()),
                auditLog.getRemark(),
                auditLog.getCreateTime());
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

    private List<String> parseDimensions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, DIMENSIONS_TYPE);
        } catch (JacksonException e) {
            log.warn("Failed to parse audit dimensions from JSON: {}", json, e);
            return List.of();
        }
    }
}

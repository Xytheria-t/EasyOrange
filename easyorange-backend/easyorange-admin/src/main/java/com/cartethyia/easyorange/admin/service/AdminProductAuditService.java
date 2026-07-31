package com.cartethyia.easyorange.admin.service;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.BatchAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.ProductAuditRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AuditLogResponse;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.BatchAuditResultResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductAuditService {

    private static final TypeReference<List<String>> DIMENSIONS_TYPE = new TypeReference<>() {};

    private final ProductRepository productRepository;
    private final ProductAuditLogRepository productAuditLogRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;
    private final AiReviewService aiReviewService;
    private final AdminProductQueryPort adminProductQueryPort;

    @Transactional(rollbackFor = Exception.class)
    public void auditProduct(String id, ProductAuditRequest request) {
        Product product = productRepository.findById(ProductId.of(id))
                .orElseThrow(() -> new ProductNotFoundException(id));
        auditOne(product, parseAction(request.action()), request.reason(), request.remark(), request.dimensions());
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchAuditResultResponse batchAudit(BatchAuditRequest request) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (BatchAuditRequest.AuditItem item : request.getItems()) {
            try {
                Product product = productRepository.findById(ProductId.of(item.productId()))
                        .orElseThrow(() -> new ProductNotFoundException(item.productId()));
                auditOne(product, parseAction(item.action()), item.reason(), null, item.dimensions());
                successCount++;
            } catch (Exception e) {
                errors.add("商品ID " + item.productId() + ": " + e.getMessage());
            }
        }

        return new BatchAuditResultResponse(request.getItems().size(), successCount, errors.size(), errors);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(String productId) {
        return productAuditLogRepository.findByProductId(productId).stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    public AiReviewResult getAiReview(String productId) {
        AdminProductQueryPort.AiReviewData data = adminProductQueryPort.getAiReviewData(productId);
        if (data == null) {
            throw new ProductNotFoundException(productId);
        }
        return aiReviewService.reviewProduct(
                data.name(),
                data.description(),
                data.categoryName(),
                data.conditionLevel(),
                data.price().toString(),
                data.sellerName(),
                data.imageUrls());
    }

    private void auditOne(Product product, AuditAction action, String reason, String remark, List<String> dimensions) {
        String operatorId = SecurityContextUtil.getCurrentUserIdOrThrow();
        String operatorName = SecurityContextUtil.getUserContext()
                .map(AuthUser::username)
                .orElse("管理员");

        String beforeStatus = product.getStatus().getCode();
        Transition<Product, ?> t = switch (action) {
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

        log.info("action=audit_product productId={} action={} operatorId={} beforeStatus={} afterStatus={}",
                product.getId().value(), action.getCode(), operatorId, beforeStatus, product.getStatus().getCode());
    }

    private static AuditAction parseAction(Integer actionCode) {
        try {
            return AuditAction.fromCode(String.valueOf(actionCode));
        } catch (IllegalArgumentException e) {
            throw BusinessException.of("无效的审核动作");
        }
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

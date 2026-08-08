package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.enums.AdminResultCode;
import com.cartethyia.easyorange.admin.domain.enums.ReportHandleAction;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort;
import com.cartethyia.easyorange.ai.dto.AiReviewResult;
import com.cartethyia.easyorange.ai.service.AiReviewService;
import com.cartethyia.easyorange.common.domain.ProductId;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.event.Transition;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.category.CategoryMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDetailMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductImageMapper;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.application.port.query.CategoryQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.application.port.query.ProductReportQueryRepository;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SellerReadModel;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.entity.ProductReport;
import com.cartethyia.easyorange.product.domain.entity.ReportHandleHistory;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.enums.ProductReportStatus;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import com.cartethyia.easyorange.product.domain.enums.ReportReasonType;
import com.cartethyia.easyorange.product.domain.event.ReportProcessedEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.repository.ReportHandleHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Admin 产品查询/操作适配器
 * <p>
 * 实现 {@link AdminProductQueryPort}，通过 Product Mapper / Repository 访问商品、审核、举报、分类数据，
 * 并转换为 Admin 模块需要的格式。
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class AdminProductQueryAdapter implements AdminProductQueryPort {

    private static final TypeReference<List<String>> DIMENSIONS_TYPE = new TypeReference<>() {};
    private static final Map<String, String> PRODUCT_STATUS_MAP;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        for (ProductStatus s : ProductStatus.values()) {
            map.put(s.getCode(), s.getDesc());
        }
        PRODUCT_STATUS_MAP = Map.copyOf(map);
    }

    private final ProductMapper productMapper;
    private final ProductDetailMapper productDetailMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final CategoryQueryRepository categoryQueryRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductReportQueryRepository productReportQueryRepository;
    private final ProductRepository productRepository;
    private final ProductAuditLogRepository productAuditLogRepository;
    private final ProductReportRepository productReportRepository;
    private final ReportHandleHistoryRepository reportHandleHistoryRepository;
    private final ProductCacheEvictionPort productCacheEvictionPort;
    private final AiReviewService aiReviewService;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    // ==================== 产品查询 ====================

    @Override
    public ProductQueryResult queryProducts(ProductQueryCondition condition) {
        var wrapper = ChainWrappers.lambdaQueryChain(productMapper).eq(ProductDO::getDelFlag, 0);

        if (condition.keyword() != null && !condition.keyword().isEmpty()) {
            wrapper.like(ProductDO::getName, condition.keyword());
        }
        if (condition.categoryId() != null) {
            wrapper.eq(ProductDO::getCategoryId, condition.categoryId());
        }
        if (condition.status() != null) {
            wrapper.eq(ProductDO::getStatus, condition.status());
        }
        if (condition.sellerId() != null) {
            wrapper.eq(ProductDO::getUserId, condition.sellerId());
        }
        if (condition.startTime() != null) {
            wrapper.ge(ProductDO::getCreateTime, condition.startTime());
        }
        if (condition.endTime() != null) {
            wrapper.le(ProductDO::getCreateTime, condition.endTime());
        }

        wrapper.orderByDesc(ProductDO::getCreateTime);

        int pageNum = condition.pageNum() != null ? condition.pageNum() : 1;
        int pageSize = condition.pageSize() != null ? condition.pageSize() : 20;
        Page<ProductDO> page = wrapper.page(new Page<>(pageNum, pageSize));

        List<ProductSummary> records =
                page.getRecords().stream().map(this::toProductSummary).collect(Collectors.toList());

        return new ProductQueryResult(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public ProductDetail getProductDetail(String productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || product.getDelFlag() != 0) {
            return null;
        }

        List<ProductDetailDO> details = productDetailMapper.selectDetailsByProductIds(List.of(productId));
        String description = details.isEmpty() ? null : details.get(0).getDescription();

        return new ProductDetail(
                product.getId(),
                product.getName(),
                description,
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getStatus() != null ? product.getStatus().getCode() : null,
                product.getStatus() != null ? product.getStatus().getDesc() : null,
                product.getConditionLevel() != null
                        ? product.getConditionLevel().getCode()
                        : null,
                product.getLocation(),
                product.getContactMethod(),
                product.getCategoryId(),
                product.getUserId(),
                product.getViewCount(),
                product.getCreateTime(),
                product.getUpdateTime());
    }

    @Override
    public Map<String, List<String>> getProductImages(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductImageDO> images = ChainWrappers.lambdaQueryChain(productImageMapper)
                .in(ProductImageDO::getProductId, productIds)
                .orderByAsc(ProductImageDO::getSortOrder)
                .list();
        return images.stream()
                .collect(Collectors.groupingBy(
                        ProductImageDO::getProductId,
                        Collectors.mapping(ProductImageDO::getImageUrl, Collectors.toList())));
    }

    @Override
    public Map<String, ProductInfo> getProductInfos(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductDO> products = productMapper.selectByIds(productIds);
        return products.stream()
                .filter(p -> p.getDelFlag() == 0)
                .collect(Collectors.toMap(ProductDO::getId, p -> new ProductInfo(p.getId(), p.getName()), (a, b) -> a));
    }

    @Override
    public AiReviewData getAiReviewData(String productId) {
        ProductDO product = productMapper.selectById(productId);
        if (product == null || product.getDelFlag() != 0) {
            return null;
        }

        List<ProductDetailDO> details = productMapper.selectDetailsByProductIds(List.of(productId));
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

    // ==================== 商品管理命令 ====================

    @Override
    public void applyProductStatus(String productId, String statusCode) {
        ProductStatus newStatus;
        try {
            newStatus = ProductStatus.fromCode(statusCode);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of("无效的商品状态");
        }

        Product product = findProductOrThrow(productId);

        Transition<Product, ?> transition =
                switch (newStatus) {
                    case ONLINE -> product.putOnline();
                    case OFFLINE -> product.takeOffline();
                    case SOLD -> product.markAsSold().orElse(null);
                    default -> throw BusinessException.of("不支持将该商品状态改为: " + newStatus.getDesc());
                };
        if (transition != null) {
            productRepository.save(transition.aggregate());
            domainEventPublisher.publish(transition.event());
        }
        productCacheEvictionPort.evictProductCache(productId);
    }

    // ==================== 审核 ====================

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

    // ==================== 举报 ====================

    @Override
    public ReportQueryResult queryReports(Integer status, Integer pageNum, Integer pageSize) {
        int page = pageNum != null ? pageNum : 1;
        int size = pageSize != null ? pageSize : 20;

        PageResult<ProductReport> reportPage =
                productReportQueryRepository.findByStatus(status != null ? String.valueOf(status) : null, page, size);

        List<ReportRecord> records = reportPage.records().stream().map(this::toReportRecord).toList();
        return new ReportQueryResult(records, reportPage.total(), page, size);
    }

    @Override
    public ReportRecord getReportDetail(String reportId) {
        ProductReport report = productReportRepository.findById(reportId);
        return report != null ? toReportRecord(report) : null;
    }

    @Override
    public List<ReportHistoryRecord> getReportHistory(String reportId) {
        return reportHandleHistoryRepository.findByReportId(reportId).stream()
                .map(this::toReportHistoryRecord)
                .toList();
    }

    @Override
    public ReportStats getReportStats() {
        return new ReportStats(
                productReportQueryRepository.countByStatus(null),
                productReportQueryRepository.countByStatus(ProductReportStatus.PENDING.getCode()),
                productReportQueryRepository.countByStatus(ProductReportStatus.PROCESSING.getCode()),
                productReportQueryRepository.countByStatus(ProductReportStatus.RESOLVED.getCode()),
                productReportQueryRepository.countByStatus(ProductReportStatus.DISMISSED.getCode()));
    }

    @Override
    public void handleReport(String reportId, String actionCode, String remark, String operatorId) {
        ProductReport report = productReportRepository.findById(reportId);
        BizRequire.notNull(report, AdminResultCode.REPORT_NOT_FOUND);
        if (!report.isPending()) {
            throw BusinessException.of(AdminResultCode.REPORT_ALREADY_HANDLED);
        }

        ReportHandleAction action = ReportHandleAction.fromCode(actionCode);
        String result = action.describe(remark);
        ProductReport updated =
                switch (action) {
                    case PRODUCT_OFFLINE -> {
                        handleProductOffline(report);
                        yield report.approve(result);
                    }
                    case BAN_PRODUCT -> {
                        handleBanProduct(report, remark);
                        yield report.approve(result);
                    }
                    case RESOLVE -> report.approve(result);
                    case DISMISS, IGNORE, WARN_SENDER -> report.reject(result);
                };

        reportHandleHistoryRepository.save(ReportHandleHistory.create(reportId, operatorId, action.getCode(), result));
        productReportRepository.update(updated);
        publishProcessedEvent(reportId, updated);
    }

    // ==================== 分类 ====================

    @Override
    public CategoryRecord getCategory(String categoryId) {
        CategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null || category.getDelFlag() != 0) {
            return null;
        }
        return toCategoryRecord(category);
    }

    @Override
    public List<CategoryRecord> listCategories(String parentId) {
        if (parentId == null) {
            return ChainWrappers.lambdaQueryChain(categoryMapper)
                    .eq(CategoryDO::getDelFlag, 0)
                    .orderByAsc(CategoryDO::getSortOrder)
                    .list()
                    .stream()
                    .map(this::toCategoryRecord)
                    .toList();
        }
        return categoryQueryRepository.findByParentId(parentId).stream().map(this::toCategoryRecord).toList();
    }

    @Override
    public List<CategoryRecord> getCategoriesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryQueryRepository.findByIds(ids).stream().map(this::toCategoryRecord).toList();
    }

    @Override
    public CategoryRecord findCategoryByName(String name) {
        CategoryReadModel existing = categoryQueryRepository.findByName(name);
        return existing != null ? toCategoryRecord(existing) : null;
    }

    @Override
    public CategoryRecord createCategory(String name, String parentId, Integer sortOrder, Integer level) {
        CategoryDO entity = CategoryDO.builder()
                .name(name)
                .parentId(parentId)
                .level(level)
                .sortOrder(sortOrder)
                .status(1)
                .build();

        categoryMapper.insert(entity);
        return toCategoryRecord(entity);
    }

    @Override
    public void updateCategory(CategoryRecord category) {
        CategoryDO entity = categoryMapper.selectById(category.id());
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("分类不存在");
        }
        entity.setName(category.name());
        entity.setParentId(category.parentId());
        entity.setLevel(category.level());
        entity.setSortOrder(category.sortOrder());
        entity.setStatus(category.status());
        categoryMapper.updateById(entity);
    }

    @Override
    public void deleteCategory(String categoryId) {
        CategoryDO entity = categoryMapper.selectById(categoryId);
        if (entity == null || entity.getDelFlag() != 0) {
            throw BusinessException.of("分类不存在");
        }
        entity.setDelFlag(2);
        categoryMapper.updateById(entity);
    }

    @Override
    public long countCategoryChildren(String categoryId) {
        return ChainWrappers.lambdaQueryChain(categoryMapper)
                .eq(CategoryDO::getParentId, categoryId)
                .eq(CategoryDO::getDelFlag, 0)
                .count();
    }

    @Override
    public Map<String, Long> countProductsByCategoryIds(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryQueryRepository.countProductsByCategoryIds(categoryIds);
    }

    // ==================== 仪表板 ====================

    @Override
    public ProductStats getProductStats() {
        return new ProductStats(
                productQueryRepository.countByStatus(null), productQueryRepository.countByStatus(ProductStatus.DRAFT.getCode()));
    }

    @Override
    public List<RecentProductRecord> getRecentProducts(int limit) {
        List<String> ids = ChainWrappers.lambdaQueryChain(productMapper)
                .eq(ProductDO::getDelFlag, 0)
                .orderByDesc(ProductDO::getCreateTime)
                .last("LIMIT " + limit)
                .list()
                .stream()
                .map(ProductDO::getId)
                .toList();

        return productQueryRepository.findProductsByIds(ids).stream()
                .map(model -> new RecentProductRecord(
                        model.id(),
                        model.sellerId(),
                        model.title(),
                        model.price(),
                        model.mainImageUrl(),
                        model.status(),
                        model.statusDesc(),
                        model.username(),
                        model.categoryName(),
                        model.views(),
                        model.createTime()))
                .toList();
    }

    @Override
    public List<TopProductRecord> getTopProducts(int limit) {
        return jdbcTemplate
                .queryForList("SELECT p.id, p.name, p.view_count, p.price, p.status, "
                        + "(SELECT pi.image_url FROM eo_product_image pi WHERE pi.product_id = p.id AND pi.del_flag = 0 ORDER BY pi.is_main DESC, pi.sort_order ASC LIMIT 1) AS main_image "
                        + "FROM eo_product p WHERE p.del_flag = 0 AND p.status = 'ONLINE' "
                        + "ORDER BY p.view_count DESC LIMIT "
                        + limit)
                .stream()
                .map(row -> {
                    String statusCode = row.get("status") != null ? row.get("status").toString() : null;
                    return new TopProductRecord(
                            String.valueOf(row.get("id")),
                            (String) row.get("name"),
                            row.get("view_count") != null ? ((Number) row.get("view_count")).intValue() : 0,
                            row.get("price") != null
                                    ? (BigDecimal) row.get("price")
                                    : BigDecimal.ZERO,
                            (String) row.get("main_image"),
                            statusCode,
                            PRODUCT_STATUS_MAP.getOrDefault(statusCode, "未知"));
                })
                .toList();
    }

    // ==================== 私有方法 ====================

    private Product findProductOrThrow(String productId) {
        return productRepository
                .findById(ProductId.of(productId))
                .orElseThrow(() -> BusinessException.of("商品不存在"));
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

    private ReportRecord toReportRecord(ProductReport report) {
        return new ReportRecord(
                report.getId(),
                report.getProductId(),
                report.getReporterId(),
                report.getReasonType(),
                reasonTypeDesc(report.getReasonType()),
                report.getReason(),
                report.statusCode(),
                statusDesc(report.statusCode()),
                report.getRemark(),
                report.getCreateTime(),
                report.getUpdateTime(),
                report.isPending());
    }

    private ReportHistoryRecord toReportHistoryRecord(ReportHandleHistory history) {
        return new ReportHistoryRecord(
                history.getId(), history.getReportId(), history.getOperatorId(), history.getAction(), history.getRemark(),
                history.getCreateTime());
    }

    private String statusDesc(String code) {
        if (code == null) {
            return null;
        }
        try {
            return ProductReportStatus.fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    private String reasonTypeDesc(String code) {
        if (code == null) {
            return null;
        }
        try {
            return ReportReasonType.fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void publishProcessedEvent(String reportId, ProductReport report) {
        domainEventPublisher.publish(new ReportProcessedEvent(
                reportId,
                report.getReporterId(),
                report.getProductId(),
                ProductReportStatus.RESOLVED.equals(report.getStatus()),
                report.getRemark(),
                LocalDateTime.now()));
    }

    private void handleProductOffline(ProductReport report) {
        var product = productRepository
                .findById(ProductId.of(report.getProductId()))
                .orElseThrow(() -> BusinessException.of(AdminResultCode.REPORT_PRODUCT_NOT_FOUND));
        productRepository.save(product.takeOffline().aggregate());
        productCacheEvictionPort.evictProductCache(report.getProductId());
    }

    private void handleBanProduct(ProductReport report, String remark) {
        var product = productRepository
                .findById(ProductId.of(report.getProductId()))
                .orElseThrow(() -> BusinessException.of(AdminResultCode.REPORT_PRODUCT_NOT_FOUND));
        productRepository.save(product.reject("举报封禁: " + remark).aggregate());
        productCacheEvictionPort.evictProductCache(report.getProductId());
    }

    private CategoryRecord toCategoryRecord(CategoryDO category) {
        return new CategoryRecord(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getLevel(),
                category.getIcon(),
                category.getSortOrder(),
                category.getStatus(),
                category.getCreateTime());
    }

    private CategoryRecord toCategoryRecord(CategoryReadModel model) {
        return new CategoryRecord(
                model.id(),
                model.name(),
                model.parentId(),
                model.level(),
                model.icon(),
                model.sortOrder(),
                model.status(),
                model.createTime());
    }

    private ProductSummary toProductSummary(ProductDO product) {
        return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getStatus() != null ? product.getStatus().getCode() : null,
                product.getStatus() != null ? product.getStatus().getDesc() : null,
                product.getConditionLevel() != null
                        ? product.getConditionLevel().getCode()
                        : null,
                product.getLocation(),
                product.getContactMethod(),
                product.getCategoryId(),
                product.getUserId(),
                product.getViewCount(),
                product.getCreateTime(),
                product.getUpdateTime());
    }
}

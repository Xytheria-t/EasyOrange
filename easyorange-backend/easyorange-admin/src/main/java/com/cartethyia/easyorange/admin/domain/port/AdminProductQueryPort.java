package com.cartethyia.easyorange.admin.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin 模块的产品查询/操作端口
 * 用于跨模块查询与操作商品、审核、举报、分类信息，遵循防腐层原则
 */
public interface AdminProductQueryPort {

    /**
     * 查询产品列表（带条件查询）
     */
    ProductQueryResult queryProducts(ProductQueryCondition condition);

    /**
     * 根据产品 ID 查询产品详情
     */
    ProductDetail getProductDetail(String productId);

    /**
     * 根据产品 ID 列表批量查询产品图片
     */
    Map<String, List<String>> getProductImages(List<String> productIds);

    /**
     * 根据产品 ID 列表批量查询产品基本信息
     */
    Map<String, ProductInfo> getProductInfos(List<String> productIds);

    /**
     * 获取 AI 审核所需数据（跨表读），产品不存在或已删除时返回 null
     */
    AiReviewData getAiReviewData(String productId);

    // ==================== 商品管理命令 ====================

    /**
     * 管理员直改商品状态（ONLINE/OFFLINE/SOLD），非法状态码/商品不存在/状态转换不允许时抛出 BusinessException
     */
    void applyProductStatus(String productId, String statusCode);

    // ==================== 审核 ====================

    /**
     * 执行商品审核（actionCode 1 通过 / 2 拒绝），持久化审核日志并发布领域事件
     */
    void auditProduct(
            String productId,
            Integer actionCode,
            String reason,
            String remark,
            List<String> dimensions,
            String operatorId,
            String operatorName);

    /**
     * 查询商品审核日志（按时间倒序）
     */
    List<AuditLogRecord> getAuditLogs(String productId);

    /**
     * AI 预审（调用 AI 审核服务），产品不存在或已删除时抛出 ProductNotFoundException
     */
    AiReviewRecord getAiReview(String productId);

    // ==================== 举报 ====================

    /**
     * 分页查询举报列表
     */
    ReportQueryResult queryReports(Integer status, Integer pageNum, Integer pageSize);

    /**
     * 查询举报详情，不存在时返回 null
     */
    ReportRecord getReportDetail(String reportId);

    /**
     * 查询举报处理历史（按时间倒序）
     */
    List<ReportHistoryRecord> getReportHistory(String reportId);

    /**
     * 举报状态统计
     */
    ReportStats getReportStats();

    /**
     * 处理单条举报：校验待处理状态、执行商品下线/封禁副作用、记录处理历史并发布领域事件
     */
    void handleReport(String reportId, String actionCode, String remark, String operatorId);

    // ==================== 分类 ====================

    /**
     * 查询分类，不存在或已删除时返回 null
     */
    CategoryRecord getCategory(String categoryId);

    /**
     * 查询分类列表（parentId 为 null 时返回全部分类，否则返回子分类），按 sortOrder 升序
     */
    List<CategoryRecord> listCategories(String parentId);

    /**
     * 按 ID 列表批量查询分类
     */
    List<CategoryRecord> getCategoriesByIds(List<String> ids);

    /**
     * 按名称查询分类（第一个匹配），不存在时返回 null
     */
    CategoryRecord findCategoryByName(String name);

    /**
     * 创建分类并返回带 ID 的分类记录
     */
    CategoryRecord createCategory(String name, String parentId, Integer sortOrder, Integer level);

    /**
     * 更新分类（全字段）
     */
    void updateCategory(CategoryRecord category);

    /**
     * 删除分类（逻辑删除），子分类/关联商品校验由调用方负责
     */
    void deleteCategory(String categoryId);

    /**
     * 统计分类的直接子分类数量
     */
    long countCategoryChildren(String categoryId);

    /**
     * 按分类 ID 列表统计关联商品数
     */
    Map<String, Long> countProductsByCategoryIds(List<String> categoryIds);

    // ==================== 仪表板 ====================

    /**
     * 商品统计：总数与待审核（DRAFT）数
     */
    ProductStats getProductStats();

    /**
     * 最近发布的商品（按创建时间倒序，limit 条）
     */
    List<RecentProductRecord> getRecentProducts(int limit);

    /**
     * 浏览量 Top 商品（仅 ONLINE，limit 条）
     */
    List<TopProductRecord> getTopProducts(int limit);

    /**
     * 产品基本信息
     */
    record ProductInfo(String id, String name) {}

    /**
     * AI 审核数据
     */
    record AiReviewData(
            String name,
            String description,
            String categoryName,
            String conditionLevel,
            BigDecimal price,
            String sellerName,
            List<String> imageUrls) {}

    /**
     * 产品查询条件
     */
    record ProductQueryCondition(
            String keyword,
            String categoryId,
            String status,
            String sellerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {}

    /**
     * 产品查询结果
     */
    record ProductQueryResult(List<ProductSummary> records, long total, int pageNum, int pageSize) {}

    /**
     * 产品摘要信息
     */
    record ProductSummary(
            String id,
            String name,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer stock,
            String status,
            String statusDesc,
            String conditionLevel,
            String location,
            String contactMethod,
            String categoryId,
            String sellerId,
            Integer viewCount,
            LocalDateTime createTime,
            LocalDateTime updateTime) {}

    /**
     * 产品详情信息
     */
    record ProductDetail(
            String id,
            String name,
            String description,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer stock,
            String status,
            String statusDesc,
            String conditionLevel,
            String location,
            String contactMethod,
            String categoryId,
            String sellerId,
            Integer viewCount,
            LocalDateTime createTime,
            LocalDateTime updateTime) {}

    /**
     * 审核日志记录 — action 为 String code（'1' 通过 / '2' 拒绝 / '3' 重提交），desc 已解析
     */
    record AuditLogRecord(
            String id,
            String productId,
            String operatorId,
            String operatorName,
            String action,
            String actionDesc,
            String reason,
            List<String> dimensions,
            String beforeStatus,
            String beforeStatusDesc,
            String afterStatus,
            String afterStatusDesc,
            String remark,
            LocalDateTime createTime) {}

    /**
     * AI 审核结果
     */
    record AiReviewRecord(
            boolean isApproved,
            String suggestedActionDesc,
            double confidenceScore,
            List<String> riskFlags,
            String reasoning) {}

    /**
     * 举报查询结果
     */
    record ReportQueryResult(List<ReportRecord> records, long total, int pageNum, int pageSize) {}

    /**
     * 举报记录 — status/reasonType 为 String code，desc 已解析
     */
    record ReportRecord(
            String id,
            String productId,
            String reporterId,
            String reasonType,
            String reasonTypeDesc,
            String reason,
            String status,
            String statusDesc,
            String remark,
            LocalDateTime createTime,
            LocalDateTime updateTime,
            boolean pending) {}

    /**
     * 举报处理历史记录
     */
    record ReportHistoryRecord(
            String id, String reportId, String operatorId, String action, String remark, LocalDateTime createTime) {}

    /**
     * 举报状态统计
     */
    record ReportStats(long total, long pending, long processing, long resolved, long dismissed) {}

    /**
     * 分类记录
     */
    record CategoryRecord(
            String id,
            String name,
            String parentId,
            Integer level,
            String icon,
            Integer sortOrder,
            Integer status,
            LocalDateTime createTime) {}

    /**
     * 商品统计
     */
    record ProductStats(long total, long pending) {}

    /**
     * 最近商品记录
     */
    record RecentProductRecord(
            String id,
            String sellerId,
            String title,
            BigDecimal price,
            String mainImageUrl,
            String status,
            String statusDesc,
            String sellerName,
            String categoryName,
            Integer views,
            LocalDateTime createTime) {}

    /**
     * Top 商品记录
     */
    record TopProductRecord(
            String productId,
            String name,
            Integer viewCount,
            BigDecimal price,
            String mainImage,
            String status,
            String statusDesc) {}
}

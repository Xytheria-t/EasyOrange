package com.cartethyia.easyorange.admin.domain.port;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 模块的评价查询端口
 * 用于跨模块查询评价信息，遵循防腐层原则
 */
public interface AdminRatingQueryPort {

    /**
     * 分页查询评价列表（带条件查询）
     */
    RatingQueryResult queryRatings(RatingQueryCondition condition);

    /**
     * 根据评价 ID 查询评价详情，不存在或已删除时返回 null
     */
    RatingSummary getRatingDetail(String ratingId);

    /**
     * 评价查询条件
     */
    record RatingQueryCondition(
            String productId,
            String userId,
            Integer rating,
            Integer status,
            String keyword,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize) {}

    /**
     * 评价查询结果
     */
    record RatingQueryResult(List<RatingSummary> records, long total, int pageNum, int pageSize) {}

    /**
     * 评价摘要信息
     */
    record RatingSummary(
            String id,
            String productId,
            String userId,
            Integer rating,
            String content,
            String replyContent,
            Integer likes,
            Integer status,
            LocalDateTime createTime,
            LocalDateTime updateTime) {}
}

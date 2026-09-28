package com.cartethyia.easyorange.message.application.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读消息统计响应项 —— 总数 + 按 {@code MessageType} 分类的未读数，供顶栏铃铛红点。
 * 分类项与 {@code UnreadCount} 一一对应，无「其它」桶：类型集合是枚举封闭的。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {

    private Long total;

    private Long systemCount;

    private Long chatCount;

    private Long orderCount;

    private Long paymentCount;

    private Long activityCount;
}

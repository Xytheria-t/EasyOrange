package com.cartethyia.easyorange.message.application.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读消息统计响应项 —— 总数 + 按 {@code MessageType} 分类的未读数，供顶栏铃铛红点。
 * 分类项与 {@code UnreadCount} 一一对应，无「其它」桶：类型集合是枚举封闭的。
 * <p>
 * 用 Integer 不用 Long：全局 Jackson 把 Long 下发成字符串（防 JS 精度丢 ID），前端类型契约是 number。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {

    private Integer total;

    private Integer systemCount;

    private Integer chatCount;

    private Integer orderCount;

    private Integer paymentCount;

    private Integer activityCount;
}

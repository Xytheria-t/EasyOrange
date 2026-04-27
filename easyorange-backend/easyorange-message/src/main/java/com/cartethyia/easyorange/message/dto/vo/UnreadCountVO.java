package com.cartethyia.easyorange.message.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读消息统计 VO
 *
 * @author cartethyia
 * @date 2026/03/06
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

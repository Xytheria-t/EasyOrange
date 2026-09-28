package com.cartethyia.easyorange.message.domain.valueobject;

/**
 * 未读消息统计值对象 —— 总数 + 按 {@code MessageType} 分类的未读数。
 * <p>
 * 取舍：读模型的一次查询直接产出该对象（{@code MessageQueryRepository#countUnreadByReceiverId}
 * 用一条 GROUP BY 返回），避免读侧为凑齐这几项发多次往返。
 * <p>
 * 边界：分类项与 {@code MessageType} 一一对应，库里出现未登记的 type 值只计入 total
 * （分类计数是 {@code getOrDefault(0)}，不因新类型上线而报错）。
 */
public record UnreadCount(
        long total, long systemCount, long chatCount, long orderCount, long paymentCount, long activityCount) {}

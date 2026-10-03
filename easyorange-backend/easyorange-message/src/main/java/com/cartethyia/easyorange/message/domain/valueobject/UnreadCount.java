package com.cartethyia.easyorange.message.domain.valueobject;

/**
 * 未读消息统计值对象 —— 总数 + 按 {@code MessageType} 分类的未读数。
 * <p>
 * 取舍：读模型的一次查询直接产出该对象（{@code MessageQueryRepository#countUnreadByReceiverId}
 * 用一条 GROUP BY 返回），避免读侧为凑齐这几项发多次往返。
 * <p>
 * 边界：分类项与 {@code MessageType} 一一对应，库里出现未登记的 type 值只计入 total
 * （分类计数是 {@code getOrDefault(0)}，不因新类型上线而报错）。
 * <p>
 * 用 int 不用 long：全局 Jackson 把 Long 序列化成字符串（防 JS 精度丢 ID），计数被殃及会下发 "2" 而非 2。
 */
public record UnreadCount(
        int total, int systemCount, int chatCount, int orderCount, int paymentCount, int activityCount) {}

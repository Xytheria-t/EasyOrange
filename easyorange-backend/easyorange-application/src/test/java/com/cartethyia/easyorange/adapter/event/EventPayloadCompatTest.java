package com.cartethyia.easyorange.adapter.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.order.domain.event.OrderPaidEvent;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.event.ProductAuditedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * 事件 JSON 载荷兼容性测试 — 锁定滚动部署窗口的向后兼容契约：
 * <ul>
 *   <li>新增字段（buyerId）在旧版消息中缺失 → 反序列化为 null，由消费者降级跳过，不报错；</li>
 *   <li>既有字段类型演进（String action → AuditAction 枚举）→ 线上格式不变，旧版消息直接兼容。</li>
 * </ul>
 */
@DisplayName("事件 JSON 载荷兼容性")
class EventPayloadCompatTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("旧版订单事件（无 buyerId）反序列化为 null 而非报错")
    void orderEvent_withoutBuyerId_deserializesToNull() throws Exception {
        var oldJson = """
                {"eventId":"evt-1","orderId":"o1","paymentStatus":"1"}
                """;

        var event = mapper.readValue(oldJson, OrderPaidEvent.class);

        assertThat(event.orderId()).isEqualTo("o1");
        assertThat(event.buyerId()).isNull();
    }

    @Test
    @DisplayName("旧版审核事件（action 为字符串码 \"1\"）反序列化为 APPROVED")
    void auditedEvent_withStringAction_deserializesToEnum() throws Exception {
        var oldJson = """
                {"eventId":"evt-2","productId":"p1","productName":"手机","sellerId":"s1","action":"1","reason":"合规","auditTime":"2026-08-14T10:00:00"}
                """;

        var event = mapper.readValue(oldJson, ProductAuditedEvent.class);

        assertThat(event.action()).isEqualTo(AuditAction.APPROVED);
        assertThat(event.reason()).isEqualTo("合规");
    }

    @Test
    @DisplayName("审核事件序列化仍输出字符串码（枚举化不改线上格式）")
    void auditedEvent_serializesToCode() throws Exception {
        var event = new ProductAuditedEvent("evt-3", "p1", "手机", "s1", AuditAction.REJECTED, "不合规", null);

        var json = mapper.writeValueAsString(event);

        assertThat(json).contains("\"action\":\"2\"");
    }
}

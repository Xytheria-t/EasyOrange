package com.cartethyia.easyorange.payment.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.payment.adapter.inbound.web.dto.response.PaymentStatusResponse;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentResponse;
import com.cartethyia.easyorange.payment.domain.aggregate.Payment;
import com.cartethyia.easyorange.payment.domain.enums.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.enums.PaymentStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentViewAssembler {

    public PaymentResponse toPaymentResponse(Payment aggregate) {
        if (aggregate == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(aggregate.id())
                .paymentNo(aggregate.paymentNo())
                .orderId(aggregate.orderId())
                .userId(aggregate.userId())
                .amount(aggregate.amount())
                .paymentMethod(aggregate.paymentMethod().getCode())
                .paymentMethodDesc(paymentMethodDesc(aggregate))
                .status(aggregate.status().getCode())
                .statusDesc(statusDesc(aggregate))
                .transactionId(aggregate.transactionId())
                .refundReason(aggregate.refundReason())
                .refundTime(aggregate.refundTime())
                .createTime(aggregate.createTime())
                .updateTime(aggregate.updateTime())
                .build();
    }

    /** 状态轻量视图的 code→desc 兜底口径必须与完整响应一致，故 desc 取值只此一处。 */
    public PaymentStatusResponse toPaymentStatusResponse(Payment aggregate) {
        return new PaymentStatusResponse(statusDesc(aggregate), paymentMethodDesc(aggregate), aggregate.updateTime());
    }

    /** 创建类命令只返回新建 ID（后端约定：create 返回 String ID），其余字段留空由前端 invalidate 重拉。 */
    public PaymentResponse toIdOnlyResponse(String paymentId) {
        return PaymentResponse.builder().id(paymentId).build();
    }

    public PageResult<PaymentResponse> toPageResult(PageResult<Payment> page) {
        List<PaymentResponse> records =
                page.records().stream().map(this::toPaymentResponse).toList();
        return PageResult.of(records, page.total(), page.current(), page.size());
    }

    private static String statusDesc(Payment aggregate) {
        return PaymentStatus.getDescByCode(aggregate.status().getCode());
    }

    private static String paymentMethodDesc(Payment aggregate) {
        return PaymentMethod.getDescByCode(aggregate.paymentMethod().getCode());
    }
}

package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PendingItemsResponse {

    private Long pendingReports;

    private Long pendingOrders;

    private Long pendingProducts;

    private List<PendingReportItem> recentReports;

    @Data
    @Builder
    public static class PendingReportItem {

        private String id;

        private String productId;

        private String productName;

        private String reason;

        private String reporterName;

        private String createTime;
    }
}

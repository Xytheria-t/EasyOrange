package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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

        private Long id;

        private Long productId;

        private String productName;

        private String reason;

        private String reporterName;

        private String createTime;
    }
}
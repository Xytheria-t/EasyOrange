package com.cartethyia.easyorange.product.adapter.inbound.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductReportResponse {

    private Long id;
    private Long productId;
    private Long reporterId;
    private String reason;
    private Integer reasonType;
    private Integer status;

    public ProductReportResponse() {
    }

    public ProductReportResponse(Long id, Long productId, Long reporterId, String reason, Integer reasonType, Integer status) {
        this.id = id;
        this.productId = productId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.reasonType = reasonType;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getReasonType() {
        return reasonType;
    }

    public void setReasonType(Integer reasonType) {
        this.reasonType = reasonType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

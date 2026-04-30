package com.cartethyia.easyorange.product.interfaces.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductReportResponse {

    private Long id;
    private Long productId;
    private Long reporterId;
    private String reason;
    private Integer status;

    public ProductReportResponse() {
    }

    public ProductReportResponse(Long id, Long productId, Long reporterId, String reason, Integer status) {
        this.id = id;
        this.productId = productId;
        this.reporterId = reporterId;
        this.reason = reason;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

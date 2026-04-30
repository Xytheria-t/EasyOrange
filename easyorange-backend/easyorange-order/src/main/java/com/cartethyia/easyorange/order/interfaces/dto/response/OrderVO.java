package com.cartethyia.easyorange.order.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long buyerId;
    private String buyerUsername;
    private Long sellerId;
    private String sellerUsername;
    private Long productId;
    private String productTitle;
    private String productImage;
    private BigDecimal amount;
    private Integer status;
    private String statusDesc;
    private String address;
    private String phone;
    private Integer quantity;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Long getBuyerId() { return buyerId; }
    public String getBuyerUsername() { return buyerUsername; }
    public Long getSellerId() { return sellerId; }
    public String getSellerUsername() { return sellerUsername; }
    public Long getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public String getProductImage() { return productImage; }
    public BigDecimal getAmount() { return amount; }
    public Integer getStatus() { return status; }
    public String getStatusDesc() { return statusDesc; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public Integer getQuantity() { return quantity; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }

    public static OrderVOBuilder builder() {
        return new OrderVOBuilder();
    }

    public static class OrderVOBuilder {
        private Long id;
        private String orderNo;
        private Long buyerId;
        private String buyerUsername;
        private Long sellerId;
        private String sellerUsername;
        private Long productId;
        private String productTitle;
        private String productImage;
        private BigDecimal amount;
        private Integer status;
        private String statusDesc;
        private String address;
        private String phone;
        private Integer quantity;
        private String remark;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        public OrderVOBuilder id(Long id) { this.id = id; return this; }
        public OrderVOBuilder orderNo(String orderNo) { this.orderNo = orderNo; return this; }
        public OrderVOBuilder buyerId(Long buyerId) { this.buyerId = buyerId; return this; }
        public OrderVOBuilder buyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; return this; }
        public OrderVOBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public OrderVOBuilder sellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; return this; }
        public OrderVOBuilder productId(Long productId) { this.productId = productId; return this; }
        public OrderVOBuilder productTitle(String productTitle) { this.productTitle = productTitle; return this; }
        public OrderVOBuilder productImage(String productImage) { this.productImage = productImage; return this; }
        public OrderVOBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public OrderVOBuilder status(Integer status) { this.status = status; return this; }
        public OrderVOBuilder statusDesc(String statusDesc) { this.statusDesc = statusDesc; return this; }
        public OrderVOBuilder address(String address) { this.address = address; return this; }
        public OrderVOBuilder phone(String phone) { this.phone = phone; return this; }
        public OrderVOBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public OrderVOBuilder remark(String remark) { this.remark = remark; return this; }
        public OrderVOBuilder createTime(LocalDateTime createTime) { this.createTime = createTime; return this; }
        public OrderVOBuilder updateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }

        public OrderVO build() {
            OrderVO vo = new OrderVO();
            vo.id = id;
            vo.orderNo = orderNo;
            vo.buyerId = buyerId;
            vo.buyerUsername = buyerUsername;
            vo.sellerId = sellerId;
            vo.sellerUsername = sellerUsername;
            vo.productId = productId;
            vo.productTitle = productTitle;
            vo.productImage = productImage;
            vo.amount = amount;
            vo.status = status;
            vo.statusDesc = statusDesc;
            vo.address = address;
            vo.phone = phone;
            vo.quantity = quantity;
            vo.remark = remark;
            vo.createTime = createTime;
            vo.updateTime = updateTime;
            return vo;
        }
    }
}

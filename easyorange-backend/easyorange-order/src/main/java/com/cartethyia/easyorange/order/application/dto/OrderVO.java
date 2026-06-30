package com.cartethyia.easyorange.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderVO {

    private String id;
    private String orderNo;
    private String buyerId;
    private String buyerUsername;
    private String sellerId;
    private String sellerUsername;
    private List<OrderItemVO> items;
    private BigDecimal totalAmount;
    private Boolean singleItem;
    private Integer status;
    private String statusDesc;
    private String address;
    private String phone;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public String getBuyerId() { return buyerId; }
    public String getBuyerUsername() { return buyerUsername; }
    public String getSellerId() { return sellerId; }
    public String getSellerUsername() { return sellerUsername; }
    public List<OrderItemVO> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Boolean getSingleItem() { return singleItem; }
    public Integer getStatus() { return status; }
    public String getStatusDesc() { return statusDesc; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }

    public static OrderVOBuilder builder() { return new OrderVOBuilder(); }

    public static class OrderVOBuilder {
        private String id;
        private String orderNo;
        private String buyerId;
        private String buyerUsername;
        private String sellerId;
        private String sellerUsername;
        private List<OrderItemVO> items;
        private BigDecimal totalAmount;
        private Boolean singleItem;
        private Integer status;
        private String statusDesc;
        private String address;
        private String phone;
        private String remark;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        public OrderVOBuilder id(String id) { this.id = id; return this; }
        public OrderVOBuilder orderNo(String orderNo) { this.orderNo = orderNo; return this; }
        public OrderVOBuilder buyerId(String buyerId) { this.buyerId = buyerId; return this; }
        public OrderVOBuilder buyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; return this; }
        public OrderVOBuilder sellerId(String sellerId) { this.sellerId = sellerId; return this; }
        public OrderVOBuilder sellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; return this; }
        public OrderVOBuilder items(List<OrderItemVO> items) { this.items = items; return this; }
        public OrderVOBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public OrderVOBuilder singleItem(Boolean singleItem) { this.singleItem = singleItem; return this; }
        public OrderVOBuilder status(Integer status) { this.status = status; return this; }
        public OrderVOBuilder statusDesc(String statusDesc) { this.statusDesc = statusDesc; return this; }
        public OrderVOBuilder address(String address) { this.address = address; return this; }
        public OrderVOBuilder phone(String phone) { this.phone = phone; return this; }
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
            vo.items = items;
            vo.totalAmount = totalAmount;
            vo.singleItem = singleItem;
            vo.status = status;
            vo.statusDesc = statusDesc;
            vo.address = address;
            vo.phone = phone;
            vo.remark = remark;
            vo.createTime = createTime;
            vo.updateTime = updateTime;
            return vo;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemVO {
        private String itemId;
        private String productId;
        private String productName;
        private String productImage;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }
}

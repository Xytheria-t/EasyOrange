package com.cartethyia.easyorange.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 订单实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_order")
public class Order extends BaseDO {

    private String orderNo;

    private Long buyerId;

    private Long sellerId;

    private Long productId;

    private BigDecimal amount;

    /**
     * 订单状态：0-待付款 1-待发货 2-待收货 3-已完成 4-已取消 5-已退款
     */
    private Integer status;

    /**
     * 支付状态：0-未支付 1-已支付 2-已退款
     */
    private Integer paymentStatus;

    private String address;

    private String phone;

    private String remark;

    public String getOrderNo() { return orderNo; }
    public Long getBuyerId() { return buyerId; }
    public Long getSellerId() { return sellerId; }
    public Long getProductId() { return productId; }
    public BigDecimal getAmount() { return amount; }
    public Integer getStatus() { return status; }
    public Integer getPaymentStatus() { return paymentStatus; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getRemark() { return remark; }

    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setStatus(Integer status) { this.status = status; }
    public void setPaymentStatus(Integer paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRemark(String remark) { this.remark = remark; }

    public Order(Long id, String orderNo, Long buyerId, Long sellerId, Long productId, BigDecimal amount,
                 Integer status, Integer paymentStatus, String address, String phone, String remark,
                 java.time.LocalDateTime createTime, java.time.LocalDateTime updateTime,
                 Long createBy, Long updateBy, Integer delFlag, Integer version) {
        super.setId(id);
        super.setCreateTime(createTime);
        super.setUpdateTime(updateTime);
        super.setCreateBy(createBy);
        super.setUpdateBy(updateBy);
        super.setDelFlag(delFlag);
        super.setVersion(version);
        this.orderNo = orderNo;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.amount = amount;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.address = address;
        this.phone = phone;
        this.remark = remark;
    }

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public static class OrderBuilder {
        private Long id;
        private String orderNo;
        private Long buyerId;
        private Long sellerId;
        private Long productId;
        private BigDecimal amount;
        private Integer status;
        private Integer paymentStatus;
        private String address;
        private String phone;
        private String remark;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
        private Long createBy;
        private Long updateBy;
        private Integer delFlag;
        private Integer version;

        public OrderBuilder id(Long id) { this.id = id; return this; }
        public OrderBuilder orderNo(String orderNo) { this.orderNo = orderNo; return this; }
        public OrderBuilder buyerId(Long buyerId) { this.buyerId = buyerId; return this; }
        public OrderBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public OrderBuilder productId(Long productId) { this.productId = productId; return this; }
        public OrderBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public OrderBuilder status(Integer status) { this.status = status; return this; }
        public OrderBuilder paymentStatus(Integer paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public OrderBuilder address(String address) { this.address = address; return this; }
        public OrderBuilder phone(String phone) { this.phone = phone; return this; }
        public OrderBuilder remark(String remark) { this.remark = remark; return this; }
        public OrderBuilder createTime(java.time.LocalDateTime createTime) { this.createTime = createTime; return this; }
        public OrderBuilder updateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; return this; }
        public OrderBuilder createBy(Long createBy) { this.createBy = createBy; return this; }
        public OrderBuilder updateBy(Long updateBy) { this.updateBy = updateBy; return this; }
        public OrderBuilder delFlag(Integer delFlag) { this.delFlag = delFlag; return this; }
        public OrderBuilder version(Integer version) { this.version = version; return this; }

        public Order build() {
            Order order = new Order(id, orderNo, buyerId, sellerId, productId, amount, status, paymentStatus, address, phone, remark, createTime, updateTime, createBy, updateBy, delFlag, version);
            return order;
        }
    }
}

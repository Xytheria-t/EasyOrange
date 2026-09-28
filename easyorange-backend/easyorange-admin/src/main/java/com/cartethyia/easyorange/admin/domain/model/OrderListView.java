package com.cartethyia.easyorange.admin.domain.model;

import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderItemInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import java.util.List;
import java.util.Map;

/**
 * 订单列表读模型 — 一页订单 + 展示要用的关联数据（买卖双方昵称、商品名）。
 * <p>
 * <b>取舍</b>：端口只给订单本身，昵称在 user 模块、商品名在 product 模块，列表要展示就得跨端口补。
 * 补齐放在服务层并整页批量取（逐条回查就是 N+1），拼成视图随分页结果一起返回 ——
 * 「这一页要补哪些关联」是业务事实，让调用方自己算一遍等于把同一段 id 收集逻辑复制到 web 层。
 * <p>
 * <b>边界</b>：只搬运端口记录，不预拼字符串；字段命名与缺省兜底（查不到就留 null）归 assembler。
 */
public record OrderListView(
        OrderQueryResult page,
        Map<String, UserInfo> users,
        Map<String, List<OrderItemInfo>> items,
        Map<String, ProductInfo> products) {}

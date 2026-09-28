package com.cartethyia.easyorange.admin.domain.model;

import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.OrderDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminOrderPort.ProductInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import java.util.Map;

/**
 * 订单详情读模型 — 订单本体 + 买卖双方档案 + 商品档案。
 * <p>
 * <b>取舍</b>：与 {@link OrderListView} 同理，详情要展示的跨模块信息在这里一次性取齐。
 * 单个订单只需 3 次查询，故按 id 逐个取，不做批量。
 * <p>
 * <b>边界</b>：买卖双方查不到时这里是 null，保留 id 让前端能显示「用户已不可见」，
 * 而不是把整块信息抹成空白。
 */
public record OrderDetailView(OrderDetail order, UserInfo buyer, UserInfo seller, Map<String, ProductInfo> products) {}

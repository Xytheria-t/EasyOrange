package com.cartethyia.easyorange.message.domain.valueobject;

import com.cartethyia.easyorange.message.domain.enums.ReadStatus;

/**
 * 消息列表查询参数 —— 分页 + 类型 / 已读态两个可选过滤。
 * <p>
 * 取舍：接收方不入参，恒取当前登录用户（列表语义就是「我的收件箱」，让调用方指定 receiverId
 * 等于开放越权读的口子）。
 * <p>
 * 边界：分页兜底是领域边界的第二层防线（第一层在 {@code PageRequest} 的字段默认值/setter）：
 * 领域对象不假设调用方一定规范化过参数，null 或不合法值一律兜到默认区间，
 * 否则下游拆箱直接 NPE（约定同 {@code OrderListQuery} / {@code PaymentListQuery} /
 * {@code ProductSearchCriteria}）。
 */
public record MessageQuery(Integer pageNum, Integer pageSize, Integer type, ReadStatus isRead) {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    public MessageQuery {
        if (pageNum == null || pageNum < 1) {
            pageNum = DEFAULT_PAGE_NUM;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
    }
}

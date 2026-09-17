package com.cartethyia.easyorange.message.domain.valueobject;

import com.cartethyia.easyorange.message.domain.enums.ReadStatus;

/**
 * Domain query parameters for message queries.
 * The application layer converts QueryMessageRequest (inbound DTO) to this domain record.
 * <p>
 * 分页兜底是领域边界的第二层防线（第一层在 {@code PageRequest} 的字段默认值/setter）：
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

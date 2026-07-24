package com.cartethyia.easyorange.message.domain.valueobject;

import com.cartethyia.easyorange.message.enums.ReadStatus;

/**
 * Domain query parameters for message queries.
 * The application layer converts QueryMessageRequest (inbound DTO) to this domain record.
 */
public record MessageQuery(
    int pageNum,
    int pageSize,
    Integer type,
    ReadStatus isRead
) {}

package com.cartethyia.easyorange.message.domain.valueobject;

/**
 * Domain value object for unread message counts by type.
 * The application layer converts this to UnreadCountVO for the controller layer.
 */
public record UnreadCount(
        long total, long systemCount, long chatCount, long orderCount, long paymentCount, long activityCount) {}

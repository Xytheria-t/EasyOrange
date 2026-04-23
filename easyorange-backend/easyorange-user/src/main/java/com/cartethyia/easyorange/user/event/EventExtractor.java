package com.cartethyia.easyorange.user.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

public interface EventExtractor<T, E extends BaseDomainEvent> {
    
    E extract(T result);
}

package com.cartethyia.easyorange.framework.outbox.converter;

import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessage;
import com.cartethyia.easyorange.framework.outbox.entity.OutboxMessagePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring", imports = {Instant.class})
public interface OutboxMessageMapper {

    OutboxMessage toDomain(OutboxMessagePO po);

    @Mapping(target = "createdAt", defaultExpression = "java(Instant.now())")
    OutboxMessagePO toPO(OutboxMessage message);
}

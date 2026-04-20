package com.cartethyia.easyorange.common.cqrs;

public interface QueryHandler<Q extends Query, R> {

    R handle(Q query);
}

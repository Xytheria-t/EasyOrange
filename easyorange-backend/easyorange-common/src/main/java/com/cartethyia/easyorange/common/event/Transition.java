package com.cartethyia.easyorange.common.event;

public record Transition<T, E extends DomainEvent>(T aggregate, E event) {}

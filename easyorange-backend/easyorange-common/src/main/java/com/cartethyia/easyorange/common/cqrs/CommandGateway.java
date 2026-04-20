package com.cartethyia.easyorange.common.cqrs;

public interface CommandGateway {

    <C extends Command, R> R execute(C command);
}

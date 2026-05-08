package com.cartethyia.easyorange.user.domain.port.output;

public interface PasswordEncoderPort extends OutboundPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}

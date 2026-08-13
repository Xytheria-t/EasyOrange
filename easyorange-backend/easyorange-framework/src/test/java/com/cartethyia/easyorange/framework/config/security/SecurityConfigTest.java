package com.cartethyia.easyorange.framework.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.nimbusds.jwt.proc.ExpiredJWTException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.jwt.BadJwtException;

@DisplayName("SecurityConfig 认证失败错误码映射")
class SecurityConfigTest {

    @Nested
    @DisplayName("resolveAuthFailureCode")
    class ResolveAuthFailureCodeTests {

        @Test
        @DisplayName("Spring 校验器过期异常（BadJwtException 消息 JWT expired）应映射 A04011")
        void springExpiredBadJwt_shouldMapToTokenExpired() {
            var ex = new InsufficientAuthenticationException(
                    "认证失败", new BadJwtException("JWT expired at 2026-08-13T00:00:00Z"));

            IResultCode code = SecurityConfig.resolveAuthFailureCode(ex);

            assertThat(code).isEqualTo(ResultCode.TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("nimbus 过期异常（cause 链含 ExpiredJWTException）应映射 A04011")
        void nimbusExpiredJwt_shouldMapToTokenExpired() {
            var ex = new InsufficientAuthenticationException("认证失败", new ExpiredJWTException("Expired JWT"));

            IResultCode code = SecurityConfig.resolveAuthFailureCode(ex);

            assertThat(code).isEqualTo(ResultCode.TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("深层 cause 链仍能识别过期（AuthenticationException → RuntimeException → ExpiredJWTException）")
        void nestedCauseChain_shouldStillDetectExpired() {
            var ex = new InsufficientAuthenticationException(
                    "认证失败", new RuntimeException("wrap", new ExpiredJWTException("Expired JWT")));

            IResultCode code = SecurityConfig.resolveAuthFailureCode(ex);

            assertThat(code).isEqualTo(ResultCode.TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("无效/缺失令牌应映射 A0401")
        void invalidToken_shouldMapToUnauthorized() {
            var ex = new InsufficientAuthenticationException("未提供令牌");

            IResultCode code = SecurityConfig.resolveAuthFailureCode(ex);

            assertThat(code).isEqualTo(ResultCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("签名错误等 BadJwtException（非过期消息）应映射 A0401")
        void badSignature_shouldMapToUnauthorized() {
            var ex = new InsufficientAuthenticationException(
                    "认证失败", new BadJwtException("JWT signature does not match locally computed signature"));

            IResultCode code = SecurityConfig.resolveAuthFailureCode(ex);

            assertThat(code).isEqualTo(ResultCode.UNAUTHORIZED);
        }
    }
}

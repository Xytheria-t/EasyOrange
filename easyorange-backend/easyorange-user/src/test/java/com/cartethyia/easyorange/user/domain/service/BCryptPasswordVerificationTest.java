package com.cartethyia.easyorange.user.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DisplayName("BCrypt 密码验证测试")
class BCryptPasswordVerificationTest {

    @Test
    @DisplayName("开发种子 admin 凭据哈希可被验证（回归护栏）")
    void verifyDevPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        String devHash = "$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK";

        // 种子 R__seed_dev_test_data.sql 写入的 admin 密码即 Password123，固定哈希必须匹配它
        assertThat(encoder.matches("Password123", devHash)).isTrue();
        // 哈希具备 BCrypt 单向性：错误密码不得匹配
        assertThat(encoder.matches("wrong-password", devHash)).isFalse();
    }

    @Test
    @DisplayName("测试环境默认哈希对应 password（回归护栏）")
    void verifyTestPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        String testHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

        assertThat(encoder.matches("password", testHash)).isTrue();
        assertThat(encoder.matches("Password123", testHash)).isFalse();
    }
}

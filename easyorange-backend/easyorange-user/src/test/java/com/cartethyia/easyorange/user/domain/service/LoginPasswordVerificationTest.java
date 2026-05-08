package com.cartethyia.easyorange.user.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("登录密码验证测试")
class LoginPasswordVerificationTest {

    @Test
    @DisplayName("验证数据库中的密码哈希是否匹配 Password123")
    void verifyDatabasePasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        String dbHash = "$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK";
        String password = "Password123";
        
        boolean matches = encoder.matches(password, dbHash);
        
        System.out.println("\n=== 登录密码验证 ===");
        System.out.println("密码: " + password);
        System.out.println("哈希: " + dbHash);
        System.out.println("匹配结果: " + matches);
        
        assertThat(matches)
            .as("密码 'Password123' 应该匹配数据库哈希")
            .isTrue();
    }
}

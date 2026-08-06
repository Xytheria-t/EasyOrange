package com.cartethyia.easyorange.user.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DisplayName("BCrypt 密码验证测试")
class BCryptPasswordVerificationTest {

    @Test
    @DisplayName("验证开发环境密码哈希值")
    void verifyDevPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        String devHash = "$2a$10$gxOyIzrDj4byMrfyopCwDOLOBdt.xlhDNjpbXDv.Au1gyApmKVDNK";

        String[] possiblePasswords = {"Password123", "password", "admin", "123456", "test", "Admin123", "PASSWORD123"};

        System.out.println("\n=== 验证开发环境密码哈希 ===");
        System.out.println("哈希值: " + devHash);

        for (String password : possiblePasswords) {
            boolean matches = encoder.matches(password, devHash);
            System.out.println(password + " -> " + matches);
            if (matches) {
                System.out.println("✓ 找到匹配的密码: " + password);
            }
        }

        String testHash = encoder.encode("Password123");
        System.out.println("\n'Password123' 的新哈希: " + testHash);
        assertThat(encoder.matches("Password123", testHash)).isTrue();
    }

    @Test
    @DisplayName("验证测试环境密码哈希值")
    void verifyTestPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        String testHash = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

        String[] possiblePasswords = {"password", "Password123", "admin", "123456", "test"};

        System.out.println("\n=== 验证测试环境密码哈希 ===");
        System.out.println("哈希值: " + testHash);

        for (String password : possiblePasswords) {
            boolean matches = encoder.matches(password, testHash);
            System.out.println(password + " -> " + matches);
            if (matches) {
                System.out.println("✓ 找到匹配的密码: " + password);
            }
        }
    }
}

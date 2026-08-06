package com.cartethyia.easyorange.config.secret;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class SecretValidationRunnerTest {

    @Mock
    private SecretService secretService;

    @Mock
    private ApplicationArguments args;

    @Test
    void throwsWhenRequiredSecretMissing() {
        when(secretService.resolve(anyString())).thenReturn(null);
        when(secretService.resolve("EASYORANGE_DB_HOST")).thenReturn("db-host");

        var runner = new SecretValidationRunner(secretService);

        assertThatThrownBy(() -> runner.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("缺少必需密钥");
    }

    @Test
    void passesWhenAllRequiredSecretsPresent() {
        when(secretService.resolve(anyString())).thenReturn("configured");

        var runner = new SecretValidationRunner(secretService);

        assertThatCode(() -> runner.run(args)).doesNotThrowAnyException();
    }
}

package com.cartethyia.easyorange.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 生产密钥契约测试 — 校验 @NotBlank 注解对空值的拦截、kebab-case 绑定正确性，
 * 以及契约仅在 prod profile 激活（dev 密钥有默认值/自动生成，不得强制校验）。
 */
class ProdSecretsTest {

    private static final Validator VALIDATOR =
            jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    private static final String[] ALL_SECRETS = {
        "easyorange.prod.db-host=db-host",
        "easyorange.prod.db-username=db-user",
        "easyorange.prod.db-password=db-pass",
        "easyorange.prod.redis-host=redis-host",
        "easyorange.prod.redis-password=redis-pass",
        "easyorange.prod.rabbitmq-host=rmq-host",
        "easyorange.prod.rabbitmq-user=rmq-user",
        "easyorange.prod.rabbitmq-password=rmq-pass",
        "easyorange.prod.jwt-rsa-private-key=private.pem",
        "easyorange.prod.jwt-rsa-public-key=public.pem"
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(SecretsConfig.class);

    /** 与生产 EasyOrangeApplication 一致的注册路径（@ConfigurationPropertiesScan 评估 @Profile 条件）。 */
    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan
    static class SecretsConfig {}

    @Test
    void bindsAllSecretsFromKebabCaseKeys() {
        var secrets = bind(Map.of(
                "easyorange.prod.db-host", "db-host",
                "easyorange.prod.db-username", "db-user",
                "easyorange.prod.db-password", "db-pass",
                "easyorange.prod.redis-host", "redis-host",
                "easyorange.prod.redis-password", "redis-pass",
                "easyorange.prod.rabbitmq-host", "rmq-host",
                "easyorange.prod.rabbitmq-user", "rmq-user",
                "easyorange.prod.rabbitmq-password", "rmq-pass",
                "easyorange.prod.jwt-rsa-private-key", "private.pem",
                "easyorange.prod.jwt-rsa-public-key", "public.pem"));

        assertThat(secrets.dbHost()).isEqualTo("db-host");
        assertThat(secrets.jwtRsaPrivateKey()).isEqualTo("private.pem");
        assertThat(VALIDATOR.validate(secrets)).isEmpty();
    }

    @Test
    void rejectsBlankSecret() {
        var secrets = bind(Map.of(
                "easyorange.prod.db-host", "db-host",
                "easyorange.prod.db-username", "db-user",
                "easyorange.prod.db-password", "",
                "easyorange.prod.redis-host", "redis-host",
                "easyorange.prod.redis-password", "redis-pass",
                "easyorange.prod.rabbitmq-host", "rmq-host",
                "easyorange.prod.rabbitmq-user", "rmq-user",
                "easyorange.prod.rabbitmq-password", "rmq-pass",
                "easyorange.prod.jwt-rsa-private-key", "private.pem",
                "easyorange.prod.jwt-rsa-public-key", "public.pem"));

        assertThat(VALIDATOR.validate(secrets))
                .anyMatch(violation -> violation.getMessage().contains("EASYORANGE_DB_PASSWORD"));
    }

    @Test
    void notActivatedOutsideProdProfile() {
        runner.withPropertyValues(ALL_SECRETS)
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).doesNotHaveBean(ProdSecrets.class));
    }

    @Test
    void failsFastWhenSecretMissingInProd() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void bindsWhenAllSecretsPresentInProd() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .withPropertyValues(ALL_SECRETS)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ProdSecrets.class);
                });
    }

    private static ProdSecrets bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind("easyorange.prod", Bindable.of(ProdSecrets.class))
                .get();
    }
}

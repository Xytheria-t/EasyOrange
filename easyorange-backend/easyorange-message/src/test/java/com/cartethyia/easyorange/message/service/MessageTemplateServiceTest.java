package com.cartethyia.easyorange.message.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.redis.RedisCache;
import com.cartethyia.easyorange.message.domain.aggregate.MessageTemplateAggregate;
import com.cartethyia.easyorange.message.domain.repository.MessageTemplateRepository;
import com.cartethyia.easyorange.message.dto.vo.MessageTemplateVO;
import com.cartethyia.easyorange.message.service.impl.MessageTemplateServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageTemplateService 单元测试")
class MessageTemplateServiceTest {

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private MessageTemplateServiceImpl templateService;

    private static final String TEMPLATE_CODE = "order_created";

    private static MessageTemplateAggregate buildTemplate(Long id, String code, String name,
                                                           String type, String title, String content,
                                                           String variables, Integer status) {
        return MessageTemplateAggregate.fromRaw(id, code, name, type, title, content, variables, status, null);
    }

    @Nested
    @DisplayName("getByCode")
    class GetByCodeTests {

        @Test
        @DisplayName("按模板编码获取模板")
        void getByCode_found_returnsTemplate() {
            MessageTemplateAggregate template = buildTemplate(1L, TEMPLATE_CODE, "test", "SYSTEM", "t", "c", null, 1);
            when(messageTemplateRepository.findByCode(TEMPLATE_CODE)).thenReturn(template);

            MessageTemplateAggregate result = templateService.getByCode(TEMPLATE_CODE);

            assertThat(result).isEqualTo(template);
        }

        @Test
        @DisplayName("模板不存在时返回 null")
        void getByCode_notFound_returnsNull() {
            when(messageTemplateRepository.findByCode(TEMPLATE_CODE)).thenReturn(null);

            MessageTemplateAggregate result = templateService.getByCode(TEMPLATE_CODE);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("renderTemplate")
    class RenderTemplateTests {

        @Test
        @DisplayName("渲染模板成功")
        void renderTemplate_success_returnsRendered() {
            MessageTemplateAggregate template = buildTemplate(
                    1L, TEMPLATE_CODE, "订单创建通知", "SYSTEM",
                    "订单 ${orderId} 已创建",
                    "您的订单 ${orderId} 已创建成功，金额 ${amount} 元",
                    "orderId,amount", 1);

            when(messageTemplateRepository.findByCode(TEMPLATE_CODE)).thenReturn(template);

            MessageTemplateVO result = templateService.renderTemplate(TEMPLATE_CODE,
                    Map.of("orderId", "12345", "amount", "99.9"));

            assertThat(result).isNotNull();
            assertThat(result.getTemplateCode()).isEqualTo(TEMPLATE_CODE);
            assertThat(result.getTitle()).isEqualTo("订单 12345 已创建");
            assertThat(result.getContent()).isEqualTo("您的订单 12345 已创建成功，金额 99.9 元");
        }

        @Test
        @DisplayName("模板不存在时抛出异常")
        void renderTemplate_notFound_throws() {
            when(messageTemplateRepository.findByCode(TEMPLATE_CODE)).thenReturn(null);

            assertThatThrownBy(() -> templateService.renderTemplate(TEMPLATE_CODE, Map.of()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("渲染模板时缺失变量保持占位符不变")
        void renderTemplate_missingVariables_keepsPlaceholder() {
            MessageTemplateAggregate template = buildTemplate(
                    1L, TEMPLATE_CODE, null, null,
                    "订单 ${orderId}",
                    "金额 ${amount} 元",
                    "orderId,amount", 1);

            when(messageTemplateRepository.findByCode(TEMPLATE_CODE)).thenReturn(template);

            MessageTemplateVO result = templateService.renderTemplate(TEMPLATE_CODE, Map.of("orderId", "12345"));

            assertThat(result.getContent()).isEqualTo("金额 ${amount} 元");
        }
    }

    @Nested
    @DisplayName("renderContent")
    class RenderContentTests {

        @Test
        @DisplayName("替换模板中的变量")
        void renderContent_replacesVariables() {
            String template = "您好 ${name}，您的验证码是 ${code}";

            String result = templateService.renderContent(template, Map.of("name", "张三", "code", "8888"));

            assertThat(result).isEqualTo("您好 张三，您的验证码是 8888");
        }

        @Test
        @DisplayName("无变量时返回原字符串")
        void renderContent_noVariables_returnsAsIs() {
            String template = "这是一个静态模板内容";

            String result = templateService.renderContent(template, Map.of());

            assertThat(result).isEqualTo(template);
        }

        @Test
        @DisplayName("模板为 null 时返回 null")
        void renderContent_nullTemplate_returnsNull() {
            String result = templateService.renderContent(null, Map.of("key", "value"));

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("变量为 null 时返回原模板")
        void renderContent_nullVariables_returnsTemplate() {
            String template = "模板 ${var}";

            String result = templateService.renderContent(template, null);

            assertThat(result).isEqualTo(template);
        }

        @Test
        @DisplayName("占位符中有特殊正则字符时安全处理")
        void renderContent_specialChars_safeReplacement() {
            String template = "金额 ${amount} 元";
            String result = templateService.renderContent(template, Map.of("amount", "99.9"));

            assertThat(result).isEqualTo("金额 99.9 元");
        }

        @Test
        @DisplayName("不存在的变量保留原占位符")
        void renderContent_unknownVariable_keepsPlaceholder() {
            String template = "用户 ${username} 您好";

            String result = templateService.renderContent(template, Map.of("other", "value"));

            assertThat(result).isEqualTo(template);
        }
    }

    @Nested
    @DisplayName("CRUD 操作")
    class CrudTests {

        @Test
        @DisplayName("查询模板列表")
        void selectTemplateList_returnsList() {
            MessageTemplateAggregate condition = buildTemplate(null, "code1", null, null, null, null, null, null);
            List<MessageTemplateAggregate> expected = List.of(
                    buildTemplate(1L, "code1", "name1", "SYSTEM", "t", "c", null, 1));
            when(messageTemplateRepository.findByCondition(condition)).thenReturn(expected);

            List<MessageTemplateAggregate> result = templateService.selectTemplateList(condition);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("新增模板")
        void insertTemplate_saves() {
            MessageTemplateAggregate template = MessageTemplateAggregate.create(
                    "code1", "name1", "SYSTEM", "t", "c", null, null);

            templateService.insertTemplate(template);

            verify(messageTemplateRepository).save(template);
        }

        @Test
        @DisplayName("更新模板")
        void updateTemplate_updates() {
            MessageTemplateAggregate template = buildTemplate(1L, "code1", "name1", "SYSTEM", "t", "c", null, 1);

            templateService.updateTemplate(template);

            verify(messageTemplateRepository).update(template);
        }

        @Test
        @DisplayName("删除模板")
        void deleteTemplateByIds_deletes() {
            Long[] ids = {1L, 2L, 3L};

            templateService.deleteTemplateByIds(ids);

            verify(messageTemplateRepository).deleteByIds(ids);
        }

        @Test
        @DisplayName("校验模板编码唯一性 — 唯一")
        void checkTemplateCodeUnique_unique_returnsTrue() {
            MessageTemplateAggregate template = buildTemplate(1L, "code1", null, null, null, null, null, null);
            when(messageTemplateRepository.existsByCodeExcludingId("code1", 1L)).thenReturn(false);

            boolean result = templateService.checkTemplateCodeUnique(template);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("校验模板编码唯一性 — 已存在")
        void checkTemplateCodeUnique_notUnique_returnsFalse() {
            MessageTemplateAggregate template = buildTemplate(1L, "code1", null, null, null, null, null, null);
            when(messageTemplateRepository.existsByCodeExcludingId("code1", 1L)).thenReturn(true);

            boolean result = templateService.checkTemplateCodeUnique(template);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("缓存操作（TODO 桩方法）")
    class CacheTests {

        @Test
        @DisplayName("缓存方法不抛出异常")
        void cacheMethods_noThrow() {
            when(messageTemplateRepository.findByCondition(any())).thenReturn(List.of());

            templateService.loadingTemplateCache();
            templateService.clearTemplateCache();
            templateService.resetTemplateCache();

            verify(redisCache, atLeastOnce()).delete(anyString());
        }
    }
}

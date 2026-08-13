package com.cartethyia.easyorange.framework.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.enums.BusinessType;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import com.cartethyia.easyorange.framework.audit.event.AuditLogEvent;
import com.cartethyia.easyorange.framework.audit.service.AuditLogService;
import com.cartethyia.easyorange.framework.config.properties.AuditLogProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogAspect 审计切面单元测试")
class AuditLogAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private HttpServletRequest request;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuditLogProperties properties;
    private AuditLogAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new AuditLogProperties();
        aspect =
                new AuditLogAspect(auditLogService, properties, objectMapper, domainEventPublisher, transactionManager);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, null));
        // lenient：读操作跳过等路径不会触达 request，避免未用桩报错
        lenient().when(request.getMethod()).thenReturn("POST");
        lenient().when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/products"));
        lenient().when(request.getQueryString()).thenReturn(null);
        lenient().when(request.getHeader(anyString())).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // ───────────────────────── Test fixtures ─────────────────────────

    /** 测试用 Controller：类名含 Product，验证模块名推导 */
    public static class ProductAuditTestController {
        public Object create(CreateRequest request) {
            return "ok";
        }

        public Object getPage(QueryRequest request) {
            return "ok";
        }
    }

    public record CreateRequest(String name, String password) {}

    public record QueryRequest(Integer page) {}

    public record LoginResult(String code, String token) {}

    private ProceedingJoinPoint joinPoint(String methodName, Object... args) throws NoSuchMethodException {
        var paramTypes = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
        var signature = mock(MethodSignature.class);
        lenient()
                .when(signature.getMethod())
                .thenReturn(ProductAuditTestController.class.getMethod(methodName, paramTypes));
        var joinPoint = mock(ProceedingJoinPoint.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(joinPoint.getTarget()).thenReturn(new ProductAuditTestController());
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private AuditLog capturePublishedAuditLog() {
        var captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        return captor.getValue().auditLog();
    }

    // ───────────────────────── 配置开关 ─────────────────────────

    @Nested
    @DisplayName("配置开关")
    class ToggleTests {

        @Test
        @DisplayName("enabled=false 时放行且不发布事件、不落库")
        void aroundLog_whenDisabled_doesNothing() throws Throwable {
            properties.setEnabled(false);
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenReturn("ok");

            var result = aspect.aroundLog(joinPoint);

            assertThat(result).isEqualTo("ok");
            verifyNoInteractions(domainEventPublisher, auditLogService);
        }
    }

    // ───────────────────────── 读操作跳过 ─────────────────────────

    @Nested
    @DisplayName("读操作跳过")
    class ReadOperationTests {

        @Test
        @DisplayName("get 前缀方法不记录审计")
        void aroundLog_whenReadOperation_skipsAudit() throws Throwable {
            var joinPoint = joinPoint("getPage", new QueryRequest(1));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.aroundLog(joinPoint);

            verifyNoInteractions(domainEventPublisher, auditLogService);
        }
    }

    // ───────────────────────── 写操作记录 ─────────────────────────

    @Nested
    @DisplayName("写操作记录")
    class WriteOperationTests {

        @Test
        @DisplayName("成功时发布事件且字段完整（模块/标题/业务类型/操作人/IP）")
        void aroundLog_whenWriteOperation_publishesCompleteAuditLog() throws Throwable {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            new AuthUser("u-1", "admin"), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.aroundLog(joinPoint);

            var auditLog = capturePublishedAuditLog();
            assertThat(auditLog.getTitle()).isEqualTo("商品管理-创建");
            assertThat(auditLog.getBusinessType()).isEqualTo(BusinessType.ADD.getCode());
            assertThat(auditLog.getMethod()).isEqualTo("ProductAuditTestController.create()");
            assertThat(auditLog.getRequestMethod()).isEqualTo("POST");
            assertThat(auditLog.getRequestUrl()).isEqualTo("http://localhost/api/products");
            assertThat(auditLog.getClientIp()).isEqualTo("127.0.0.1");
            assertThat(auditLog.getUsername()).isEqualTo("admin");
            assertThat(auditLog.getOperatorType()).isEqualTo(1);
            assertThat(auditLog.getStatus()).isZero();
            assertThat(auditLog.getDuration()).isGreaterThanOrEqualTo(0);
            verify(auditLogService, never()).insertAuditLog(any());
        }

        @Test
        @DisplayName("普通用户（无 ROLE_ADMIN 权限）operatorType=2")
        void aroundLog_whenNormalUser_marksOperatorUser() throws Throwable {
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(new AuthUser("u-2", "zhangsan"), null, List.of()));
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.aroundLog(joinPoint);

            var auditLog = capturePublishedAuditLog();
            assertThat(auditLog.getUsername()).isEqualTo("zhangsan");
            assertThat(auditLog.getOperatorType()).isEqualTo(2);
        }

        @Test
        @DisplayName("未登录时 operatorType=0、username=anonymous")
        void aroundLog_whenAnonymous_marksOperatorAnonymous() throws Throwable {
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.aroundLog(joinPoint);

            var auditLog = capturePublishedAuditLog();
            assertThat(auditLog.getUsername()).isEqualTo("anonymous");
            assertThat(auditLog.getOperatorType()).isZero();
        }

        @Test
        @DisplayName("业务异常时记录失败状态并继续抛出")
        void aroundLog_whenException_recordsFailureAndRethrows() throws Throwable {
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));

            assertThatThrownBy(() -> aspect.aroundLog(joinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("boom");

            var auditLog = capturePublishedAuditLog();
            assertThat(auditLog.getStatus()).isEqualTo(1);
            assertThat(auditLog.getErrorMsg()).isEqualTo("boom");
        }

        @Test
        @DisplayName("请求参数序列化且敏感字段被掩码")
        void aroundLog_requestParams_masked() throws Throwable {
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.aroundLog(joinPoint);

            var params = capturePublishedAuditLog().getRequestParams();
            assertThat(params).contains("手机").contains("******").doesNotContain("p@ssw0rd");
        }

        @Test
        @DisplayName("save-request-data=false 时不保存请求参数")
        void aroundLog_whenSaveRequestDataDisabled_paramsNull() throws Throwable {
            properties.setSaveRequestData(false);
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenReturn("ok");

            aspect.aroundLog(joinPoint);

            assertThat(capturePublishedAuditLog().getRequestParams()).isNull();
        }

        @Test
        @DisplayName("save-response-data=true 时响应数据被掩码")
        void aroundLog_whenSaveResponseData_responseMasked() throws Throwable {
            properties.setSaveResponseData(true);
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenReturn(new LoginResult("A0000", "jwt-token-123"));

            aspect.aroundLog(joinPoint);

            var response = capturePublishedAuditLog().getResponseData();
            assertThat(response).contains("******").doesNotContain("jwt-token-123");
        }
    }

    // ───────────────────────── Outbox 发布失败降级 ─────────────────────────

    @Nested
    @DisplayName("Outbox 发布失败降级")
    class FallbackTests {

        @Test
        @DisplayName("事件发布异常时降级为直接入库")
        void publishAuditLog_whenPublishFails_fallsBackToDirectInsert() throws Throwable {
            var joinPoint = joinPoint("create", new CreateRequest("手机", "p@ssw0rd"));
            when(joinPoint.proceed()).thenReturn("ok");
            doThrow(new RuntimeException("mq down")).when(domainEventPublisher).publish(any());

            aspect.aroundLog(joinPoint);

            var captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogService).insertAuditLog(captor.capture());
            assertThat(captor.getValue().getMethod()).isEqualTo("ProductAuditTestController.create()");
            assertThat(captor.getValue().getTitle()).isEqualTo("商品管理-创建");
        }
    }
}

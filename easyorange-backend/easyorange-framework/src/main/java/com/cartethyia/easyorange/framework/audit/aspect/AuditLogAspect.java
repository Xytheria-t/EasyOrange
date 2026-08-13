package com.cartethyia.easyorange.framework.audit.aspect;

import com.cartethyia.easyorange.common.enums.BusinessType;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.security.AuthUser;
import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import com.cartethyia.easyorange.framework.audit.event.AuditLogEvent;
import com.cartethyia.easyorange.framework.audit.service.AuditLogService;
import com.cartethyia.easyorange.framework.config.properties.AuditLogProperties;
import com.cartethyia.easyorange.framework.util.AuditLogUtil;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 审计日志 AOP 切面。
 * <p>
 * 拦截所有 RestController 的写操作方法，通过 Spring Modulith Outbox 异步记录审计日志。
 * 约定优于注解：方法名以 get/query/find/list 等前缀开头视为读操作，跳过记录。
 * 模块名、操作标题和业务类型通过 {@link AuditLogProperties} 的映射表推导。
 * </p>
 * <p>
 * 事件流：切面发布 {@link AuditLogEvent} → Modulith 写入 {@code EVENT_PUBLICATION} 表
 * （与当前事务同提交，Outbox 模式保证崩溃不丢）→ RabbitMQ 异步投递 →
 * {@code AuditLogEventConsumer} 消费写库。若事件发布失败，降级为直接入库（best-effort）。
 * </p>
 * <p>
 * 执行顺序 {@code @Order(3)}，在幂等/限流等 Filter 层拦截生效之后，
 * 确保防护拦截在审计日志记录前执行。
 * </p>
 */
@Slf4j
@Aspect
@Order(3)
@Component
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final AuditLogProperties auditLogProperties;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final TransactionTemplate transactionTemplate;

    public AuditLogAspect(
            AuditLogService auditLogService,
            AuditLogProperties auditLogProperties,
            ObjectMapper objectMapper,
            DomainEventPublisher domainEventPublisher,
            PlatformTransactionManager transactionManager) {
        this.auditLogService = auditLogService;
        this.auditLogProperties = auditLogProperties;
        this.objectMapper = objectMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ───────────────────────── Constants ─────────────────────────

    /** 操作状态：正常（对齐 {@link AuditLog#getStatus()} 注释） */
    private static final int STATUS_SUCCESS = 0;
    /** 操作状态：异常 */
    private static final int STATUS_FAILURE = 1;
    /** 操作类别：管理员（1=管理员, 2=普通用户, 0=未登录，见 {@link AuditLog#getOperatorType()}） */
    private static final int OPERATOR_ADMIN = 1;
    /** 操作类别：普通用户 */
    private static final int OPERATOR_USER = 2;
    /** 操作类别：未登录 */
    private static final int OPERATOR_ANONYMOUS = 0;

    // ───────────────────────── Pointcut ─────────────────────────

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerPointcut() {}

    // ───────────────────────── Around advice ─────────────────────────

    @Around("restControllerPointcut()")
    public Object aroundLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            handleLog(joinPoint, null, result, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            handleLog(joinPoint, e, null, System.currentTimeMillis() - startTime);
            throw e;
        }
    }

    private void handleLog(ProceedingJoinPoint joinPoint, Exception e, Object jsonResult, long costTime) {
        if (!auditLogProperties.isEnabled()) {
            return;
        }
        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();

            if (isReadOperation(methodName)) {
                return;
            }

            HttpServletRequest request = RequestUtil.getRequest();
            if (request == null) return;

            AuditLog auditLog = buildAuditLog(joinPoint, methodName, e, jsonResult, request, costTime);
            publishAuditLog(auditLog);

        } catch (Exception exp) {
            log.error("Failed to save audit log", exp);
        }
    }

    // ───────────────────────── Read operation detection ─────────────────────────

    private boolean isReadOperation(String methodName) {
        if (methodName == null || methodName.isEmpty()) return false;
        for (String prefix : auditLogProperties.getSkipPrefixes()) {
            if (methodName.startsWith(prefix)) return true;
        }
        return false;
    }

    // ───────────────────────── Event publishing (Outbox) ─────────────────────────

    /**
     * 通过 Spring Modulith Outbox 发布审计日志事件。
     * <p>
     * 事件在独立短事务中发布 → Modulith 写入 {@code EVENT_PUBLICATION} 表 →
     * 提交后异步投递 RabbitMQ → 消费者写库。若发布失败，降级为直接入库。
     */
    private void publishAuditLog(AuditLog auditLog) {
        try {
            transactionTemplate.executeWithoutResult(_ -> domainEventPublisher.publish(AuditLogEvent.of(auditLog)));
        } catch (Exception e) {
            log.warn("Outbox 发布审计日志失败，降级为直接入库: method={}", auditLog.getMethod(), e);
            auditLogService.insertAuditLog(auditLog);
        }
    }

    // ───────────────────────── AuditLog builder ─────────────────────────

    private AuditLog buildAuditLog(
            ProceedingJoinPoint joinPoint,
            String methodName,
            Exception e,
            Object jsonResult,
            HttpServletRequest request,
            long costTime) {

        String className = joinPoint.getTarget().getClass().getSimpleName();

        String moduleName = deriveModuleName(className);
        var rule = auditLogProperties.findMapping(methodName).orElse(null);
        String operationTitle = rule != null ? rule.title() : methodName;
        BusinessType businessType = rule != null ? rule.businessType() : BusinessType.OTHER;

        var userCtx = SecurityContextUtil.getUserContext();

        var builder = AuditLog.builder()
                .title(moduleName + "-" + operationTitle)
                .businessType(businessType.getCode())
                .method(className + "." + methodName + "()")
                .requestMethod(request.getMethod())
                .requestUrl(RequestUtil.getFullRequestUrl(request))
                .clientIp(RequestUtil.getClientIp(request))
                .username(userCtx.map(AuthUser::username).orElse("anonymous"))
                .operatorType(resolveOperatorType(userCtx.orElse(null)))
                .status(e != null ? STATUS_FAILURE : STATUS_SUCCESS)
                .errorMsg(e != null ? AuditLogUtil.truncate(e.getMessage(), 2000) : null)
                .createdAt(LocalDateTime.now())
                .duration(Math.toIntExact(costTime));

        if (auditLogProperties.isSaveRequestData()) {
            String params = argsArrayToString(joinPoint.getArgs());
            builder.requestParams(AuditLogUtil.truncate(params, 2000));
        }

        if (auditLogProperties.isSaveResponseData() && jsonResult != null) {
            try {
                // 与请求参数一致，响应数据同样做敏感字段掩码
                String json = maskSensitiveFields(objectMapper.writeValueAsString(jsonResult));
                builder.responseData(AuditLogUtil.truncate(json, 2000));
            } catch (JacksonException ex) {
                log.warn("Failed to serialize response data for audit log", ex);
            }
        }

        return builder.build();
    }

    // ───────────────────────── Convention helpers ─────────────────────────

    private String deriveModuleName(String className) {
        if (className == null || className.isEmpty()) return className;

        String lookup =
                className.replace("Controller", "").replace("Command", "").replace("Query", "");

        Map<String, String> mapping = auditLogProperties.getModuleNames();
        String bestMatch = null;
        int bestLen = 0;
        for (var entry : mapping.entrySet()) {
            if (lookup.contains(entry.getKey()) && entry.getKey().length() > bestLen) {
                bestMatch = entry.getValue();
                bestLen = entry.getKey().length();
            }
        }
        return bestMatch != null ? bestMatch : lookup;
    }

    private static int resolveOperatorType(AuthUser userCtx) {
        if (userCtx == null) {
            return OPERATOR_ANONYMOUS;
        }
        // 角色不存于 AuthUser，改由已授予的 authorities（源自 JWT claim）判定
        var authorities = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getAuthorities)
                .orElseGet(List::of);
        return authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                ? OPERATOR_ADMIN
                : OPERATOR_USER;
    }

    // ───────────────────────── Request param handling ─────────────────────────

    private String argsArrayToString(Object[] paramsArray) {
        if (paramsArray == null || paramsArray.length == 0) {
            return "";
        }

        var params = new StringBuilder();
        for (Object value : paramsArray) {
            if (value == null || isFilterObject(value)) continue;

            try {
                String json = objectMapper.writeValueAsString(value);
                String maskedJson = maskSensitiveFields(json);
                params.append(maskedJson).append(" ");
            } catch (JacksonException ignored) {
                // skip un-serializable arguments
            }
        }
        return params.toString().trim();
    }

    private static boolean isFilterObject(Object obj) {
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class)
                    || clazz.getComponentType().isAssignableFrom(HttpServletRequest.class);
        }
        return obj instanceof MultipartFile
                || obj instanceof HttpServletRequest
                || obj instanceof jakarta.servlet.http.HttpServletResponse
                || obj instanceof BindingResult;
    }

    // ───────────────────────── Sensitive field masking ─────────────────────────

    private String maskSensitiveFields(String json) {
        if (json == null || json.isEmpty()) return json;
        List<String> sensitiveFields = auditLogProperties.getSensitiveFields();
        if (sensitiveFields == null || sensitiveFields.isEmpty()) return json;
        // 快速路径：原始 JSON 不含任何敏感字段名，无需解析掩码（避免整棵树反序列化）
        if (sensitiveFields.stream().noneMatch(json::contains)) return json;

        try {
            JsonNode root = objectMapper.readTree(json);
            maskNode(root, sensitiveFields);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    private void maskNode(JsonNode node, List<String> sensitiveFields) {
        if (node.isObject()) {
            var objectNode = (ObjectNode) node;
            node.propertyNames().forEach(fieldName -> {
                if (sensitiveFields.contains(fieldName)) {
                    objectNode.set(fieldName, objectMapper.valueToTree("******"));
                } else {
                    JsonNode value = node.get(fieldName);
                    if (value.isObject() || value.isArray()) {
                        maskNode(value, sensitiveFields);
                    }
                }
            });
        } else if (node.isArray()) {
            node.forEach(child -> maskNode(child, sensitiveFields));
        }
    }
}

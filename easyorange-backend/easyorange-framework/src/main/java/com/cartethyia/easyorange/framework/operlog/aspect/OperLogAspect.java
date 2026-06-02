package com.cartethyia.easyorange.framework.operlog.aspect;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.enums.BusinessType;
import com.cartethyia.easyorange.framework.config.properties.OperLogProperties;
import com.cartethyia.easyorange.framework.operlog.entity.SysOperLog;
import com.cartethyia.easyorange.framework.operlog.service.SysOperLogService;
import com.cartethyia.easyorange.framework.util.OperLogUtil;
import com.cartethyia.easyorange.framework.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Aspect
@Order(3)
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogService sysOperLogService;
    private final OperLogProperties operLogProperties;
    private final ObjectMapper objectMapper;

    private static final Set<String> DEFAULT_SENSITIVE_FIELDS = Set.of(
            "password", "confirmPassword", "oldPassword", "newPassword",
            "token", "secret", "secretKey", "accessToken", "refreshToken"
    );

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerPointcut() {}

    @AfterReturning(pointcut = "restControllerPointcut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    @AfterThrowing(pointcut = "restControllerPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    private void handleLog(JoinPoint joinPoint, Exception e, Object jsonResult) {
        if (!operLogProperties.isEnabled()) {
            return;
        }

        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            if (!shouldLogByConvention(method)) {
                return;
            }

            HttpServletRequest request = RequestUtil.getRequest();
            if (request == null) {
                return;
            }

            SysOperLog operLog = buildOperLog(joinPoint, method, e, jsonResult, request);
            sysOperLogService.insertOperLog(operLog);

        } catch (Exception exp) {
            log.error("Failed to save operation log", exp);
        }
    }

    private boolean shouldLogByConvention(Method method) {
        String name = method.getName();
        return !(name.startsWith("get") || name.startsWith("query") || name.startsWith("find")
                || name.startsWith("list") || name.startsWith("detail") || name.startsWith("search")
                || name.startsWith("count") || name.startsWith("check") || name.startsWith("exists")
                || name.startsWith("stats") || name.startsWith("my"));
    }

    private SysOperLog buildOperLog(JoinPoint joinPoint, Method method,
                                     Exception e, Object jsonResult,
                                     HttpServletRequest request) {

        String className = joinPoint.getTarget().getClass().getSimpleName();

        String moduleName = OperLogUtil.deriveModuleName(className);
        String operationTitle = OperLogUtil.deriveOperationTitle(method.getName());
        BusinessType businessType = deriveBusinessType(method.getName());

        SysOperLog operLog = new SysOperLog();
        operLog.setTitle(moduleName + "-" + operationTitle);
        operLog.setBusinessType(businessType.getCode());
        operLog.setMethod(className + "." + method.getName() + "()");
        operLog.setRequestMethod(request.getMethod());
        operLog.setOperUrl(RequestUtil.getFullRequestUrl(request));
        operLog.setOperIp(RequestUtil.getClientIp(request));
        operLog.setOperName(SecurityContextUtil.getUserContext()
            .map(AuthUser::username)
            .orElse("anonymous"));

        if (operLogProperties.isSaveRequestData()) {
            String params = argsArrayToString(joinPoint.getArgs());
            operLog.setOperParam(OperLogUtil.truncate(params, 2000));
        }

        if (operLogProperties.isSaveResponseData() && jsonResult != null) {
            try {
                String json = objectMapper.writeValueAsString(jsonResult);
                operLog.setJsonResult(OperLogUtil.truncate(json, 2000));
            } catch (JacksonException ex) {
                log.warn("Failed to serialize response data for oper log", ex);
            }
        }

        if (e != null) {
            operLog.setStatus(1);
            operLog.setErrorMsg(OperLogUtil.truncate(e.getMessage(), 2000));
        } else {
            operLog.setStatus(0);
        }

        operLog.setOperTime(LocalDateTime.now());

        var startTime = (Long) request.getAttribute("requestStartTime");
        operLog.setCostTime((int)(System.currentTimeMillis() - (startTime != null ? startTime : System.currentTimeMillis())));

        return operLog;
    }

    private BusinessType deriveBusinessType(String methodName) {
        if (methodName.startsWith("create") || methodName.startsWith("add") || methodName.startsWith("save")
            || methodName.startsWith("register") || methodName.startsWith("upload") || methodName.startsWith("import")) {
            return BusinessType.ADD;
        }
        if (methodName.startsWith("update") || methodName.startsWith("edit") || methodName.startsWith("modify")
            || methodName.startsWith("change") || methodName.startsWith("reset") || methodName.startsWith("mark")
            || methodName.startsWith("approve") || methodName.startsWith("reject") || methodName.startsWith("process")
            || methodName.startsWith("handle") || methodName.startsWith("bind") || methodName.startsWith("unbind")
            || methodName.startsWith("toggle") || methodName.startsWith("audit") || methodName.startsWith("enable")
            || methodName.startsWith("disable") || methodName.startsWith("ban") || methodName.startsWith("unban")
            || methodName.startsWith("force") || methodName.startsWith("unlock") || methodName.startsWith("recall")
            || methodName.startsWith("send") || methodName.startsWith("typing") || methodName.startsWith("reply")
            || methodName.startsWith("like") || methodName.startsWith("report")) {
            return BusinessType.UPDATE;
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove") || methodName.startsWith("cancel")) {
            return BusinessType.DELETE;
        }
        if (methodName.startsWith("login") || methodName.startsWith("logout")) {
            return BusinessType.LOGIN;
        }
        return BusinessType.OTHER;
    }

    private String argsArrayToString(Object[] paramsArray) {
        if (paramsArray == null || paramsArray.length == 0) {
            return "";
        }

        var params = new StringBuilder();
        for (Object value : paramsArray) {
            if (value == null) {
                continue;
            }
            if (isFilterObject(value)) {
                continue;
            }

            String json;
            try {
                json = objectMapper.writeValueAsString(value);
            } catch (JacksonException e) {
                continue;
            }
            if (json != null) {
                String maskedJson = maskSensitiveFields(json);
                params.append(maskedJson).append(" ");
            }
        }
        return params.toString().trim();
    }

    private boolean isFilterObject(Object obj) {
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

    private String maskSensitiveFields(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            maskNode(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    private void maskNode(JsonNode node) {
        if (node.isObject()) {
            var objectNode = (ObjectNode) node;
            node.propertyNames().forEach(fieldName -> {
                JsonNode value = node.get(fieldName);

                if (DEFAULT_SENSITIVE_FIELDS.contains(fieldName)) {
                    objectNode.set(fieldName, objectMapper.valueToTree("******"));
                } else if (value.isObject() || value.isArray()) {
                    maskNode(value);
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::maskNode);
        }
    }
}

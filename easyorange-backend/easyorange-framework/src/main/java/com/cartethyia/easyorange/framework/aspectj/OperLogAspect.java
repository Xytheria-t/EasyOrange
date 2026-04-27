package com.cartethyia.easyorange.framework.aspectj;

import com.cartethyia.easyorange.common.annotation.Log;
import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.enums.BusinessType;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.framework.config.properties.OperLogProperties;
import com.cartethyia.easyorange.framework.operlog.entity.SysOperLog;
import com.cartethyia.easyorange.framework.operlog.service.SysOperLogService;
import com.cartethyia.easyorange.framework.util.OperLogUtil;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Slf4j
@Aspect
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

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restControllerPointcut() {}

    @AfterReturning(pointcut = "restControllerPointcut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    @AfterThrowing(pointcut = "restControllerPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    private void handleLog(final JoinPoint joinPoint, final Exception e, final Object jsonResult) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            Log controllerLog = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), Log.class);
            Log methodLog = AnnotationUtils.findAnnotation(method, Log.class);

            if (controllerLog == null && methodLog == null && !shouldLogByConvention(method)) {
                return;
            }

            if (!operLogProperties.isEnabled()) {
                return;
            }

            HttpServletRequest request = RequestUtil.getRequest();
            if (request == null) {
                return;
            }

            SysOperLog operLog = buildOperLog(joinPoint, method, e, jsonResult, controllerLog, methodLog, request);
            sysOperLogService.insertOperLog(operLog);

        } catch (Exception exp) {
            log.error("Failed to save operation log", exp);
        }
    }

    private boolean shouldLogByConvention(Method method) {
        String methodName = method.getName();
        return !methodName.startsWith("get")
            && !methodName.startsWith("query")
            && !methodName.startsWith("find")
            && !methodName.startsWith("list")
            && !methodName.startsWith("detail")
            && !methodName.startsWith("search")
            && !methodName.startsWith("count")
            && !methodName.startsWith("check")
            && !methodName.startsWith("exists");
    }

    private SysOperLog buildOperLog(JoinPoint joinPoint, Method method,
                                     Exception e, Object jsonResult,
                                     Log controllerLog, Log methodLog,
                                     HttpServletRequest request) {

        Log effectiveLog = methodLog != null ? methodLog : controllerLog;
        String className = joinPoint.getTarget().getClass().getSimpleName();

        String moduleName;
        String operationTitle;
        BusinessType businessType;
        boolean saveRequestData;
        boolean saveResponseData;
        String[] excludeParamNames;

        if (effectiveLog != null) {
            String title = effectiveLog.title();
            if (title.isEmpty()) {
                moduleName = OperLogUtil.deriveModuleName(className);
                operationTitle = OperLogUtil.deriveOperationTitle(method.getName());
            } else if (title.contains("-")) {
                int dashIndex = title.indexOf("-");
                moduleName = title.substring(0, dashIndex);
                operationTitle = title.substring(dashIndex + 1);
            } else {
                moduleName = title;
                operationTitle = OperLogUtil.deriveOperationTitle(method.getName());
            }
            businessType = effectiveLog.type();
            saveRequestData = effectiveLog.isSaveRequestData();
            saveResponseData = effectiveLog.isSaveResponseData();
            excludeParamNames = effectiveLog.excludeParamNames();
        } else {
            moduleName = OperLogUtil.deriveModuleName(className);
            operationTitle = OperLogUtil.deriveOperationTitle(method.getName());
            businessType = deriveBusinessType(method.getName());
            saveRequestData = operLogProperties.isSaveRequestData();
            saveResponseData = operLogProperties.isSaveResponseData();
            excludeParamNames = new String[0];
        }

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

        if (saveRequestData) {
            String params = argsArrayToString(joinPoint.getArgs(), excludeParamNames);
            operLog.setOperParam(OperLogUtil.truncate(params, 2000));
        }

        if (saveResponseData && jsonResult != null) {
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

        Long startTime = (Long) request.getAttribute("requestStartTime");
        operLog.setCostTime(System.currentTimeMillis() - (startTime != null ? startTime : System.currentTimeMillis()));

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
            || methodName.startsWith("handle") || methodName.startsWith("bind") || methodName.startsWith("unbind")) {
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

    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames) {
        if (paramsArray == null || paramsArray.length == 0) {
            return "";
        }

        StringBuilder params = new StringBuilder();
        for (Object value : paramsArray) {
            if (value == null) {
                continue;
            }
            if (isFilterObject(value)) {
                continue;
            }

            String objJson;
            try {
                objJson = objectMapper.writeValueAsString(value);
            } catch (JacksonException e) {
                continue;
            }
            if (objJson != null) {
                String maskedJson = maskSensitiveFields(objJson, excludeParamNames);
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

    private String maskSensitiveFields(String json, String[] extraExcludeNames) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            maskNode(root, extraExcludeNames);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    private void maskNode(JsonNode node, String[] extraExcludeNames) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            node.propertyNames().forEach(fieldName -> {
                JsonNode value = node.get(fieldName);

                if (isSensitiveField(fieldName, extraExcludeNames)) {
                    objectNode.set(fieldName, objectMapper.valueToTree("******"));
                } else if (value.isObject() || value.isArray()) {
                    maskNode(value, extraExcludeNames);
                }
            });
        } else if (node.isArray()) {
            node.forEach(child -> maskNode(child, extraExcludeNames));
        }
    }

    private boolean isSensitiveField(String fieldName, String[] extraExcludeNames) {
        if (DEFAULT_SENSITIVE_FIELDS.contains(fieldName)) {
            return true;
        }
        if (extraExcludeNames != null) {
            for (String exclude : extraExcludeNames) {
                if (fieldName.equalsIgnoreCase(exclude)) {
                    return true;
                }
            }
        }
        return false;
    }
}

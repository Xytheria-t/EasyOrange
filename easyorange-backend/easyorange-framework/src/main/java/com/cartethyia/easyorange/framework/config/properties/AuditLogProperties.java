package com.cartethyia.easyorange.framework.config.properties;

import com.cartethyia.easyorange.common.enums.BusinessType;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * 审计日志配置属性（prefix=audit）— 是否启用、保留天数、是否落请求 / 响应数据、跳过的读方法前缀、掩码字段、
 * 模块名与操作映射。
 * <p>
 * {@code methodMappings} 按方法名前缀<b>单表</b>同时推导操作标题与业务类型，避免标题映射与类型映射两套表各自
 * 维护而漂移；{@code moduleNames} 同为长优先匹配。
 *
 * @param enabled 是否启用审计日志记录；false 时完全禁用日志记录功能
 * @param retentionDays 审计日志保留天数，超期由 AuditLogCleanupTask 每日清理
 * @param saveRequestData 是否保存请求参数到 oper_param 字段
 * @param saveResponseData 是否保存 JSON 响应到 json_result 字段；会增加数据库存储压力，生产环境建议保持关闭
 * @param skipPrefixes 不记录日志的读操作方法名前缀列表，方法名以这些前缀开头时跳过，默认覆盖常见查询前缀
 * @param sensitiveFields 请求参数中需要掩码的敏感字段名列表，记录时值被替换为 ******
 * @param moduleNames Controller 类名 → 中文模块名称映射，用于推导审计日志的模块字段，按长优先匹配
 *     （如 "ProductAudit" 优先于 "Product"）
 * @param methodMappings 方法名前缀 → 操作映射（标题 + 业务类型）；审计日志推导的单一事实来源
 */
@Validated
@ConfigurationProperties(prefix = "audit")
public record AuditLogProperties(
        @DefaultValue("true") boolean enabled,
        @Min(1) @DefaultValue("180") int retentionDays,
        @DefaultValue("true") boolean saveRequestData,
        @DefaultValue("false") boolean saveResponseData,
        List<String> skipPrefixes,
        List<String> sensitiveFields,
        Map<String, String> moduleNames,
        Map<String, MethodMapping> methodMappings) {

    public AuditLogProperties {
        skipPrefixes = skipPrefixes == null
                ? List.of("get", "query", "find", "list", "detail", "search", "count", "check", "exists", "stats", "my")
                : List.copyOf(skipPrefixes);
        sensitiveFields = sensitiveFields == null
                ? List.of(
                        "password",
                        "confirmPassword",
                        "oldPassword",
                        "newPassword",
                        "token",
                        "secret",
                        "secretKey",
                        "accessToken",
                        "refreshToken")
                : List.copyOf(sensitiveFields);
        moduleNames = Map.copyOf(moduleNames == null ? defaultModuleNames() : moduleNames);
        methodMappings = Map.copyOf(methodMappings == null ? defaultMethodMappings() : methodMappings);
    }

    private static Map<String, String> defaultModuleNames() {
        var map = new LinkedHashMap<String, String>();
        map.put("Product", "商品管理");
        map.put("User", "用户管理");
        map.put("Auth", "认证管理");
        map.put("Order", "订单管理");
        map.put("Message", "消息管理");
        map.put("Payment", "支付管理");
        map.put("File", "文件管理");
        map.put("Search", "搜索管理");
        map.put("Health", "系统健康");
        map.put("Menu", "菜单管理");
        map.put("Role", "角色管理");
        map.put("Dept", "部门管理");
        map.put("Dict", "字典管理");
        map.put("Config", "配置管理");
        map.put("Notice", "通知公告");
        map.put("LoginLog", "登录日志");
        map.put("AuditLog", "审计日志");
        // 匹配是「类名去 Controller/Command/Query 后找最长的键包含于其中」，所以比 "AiKnowledge" 更长的
        // 键会被优先命中；少了下面这些，审计记录的模块名会回退成英文类名
        map.put("Category", "分类管理");
        map.put("Dashboard", "仪表盘");
        map.put("AiChat", "AI 对话");
        map.put("AiFeedback", "AI 反馈");
        map.put("AiKnowledge", "AI 知识库");
        map.put("AiListing", "AI 发布助手");
        map.put("AiAdminCostReport", "AI 成本报表");
        map.put("AiAdminFeedbackExport", "AI 反馈导出");
        map.put("AiAdminKnowledge", "AI 知识库管理");
        map.put("AiAdminListingAdoption", "AI 采纳率");
        map.put("AiAdminRetrievalEval", "AI 检索评测");
        return map;
    }

    private static Map<String, MethodMapping> defaultMethodMappings() {
        var map = new LinkedHashMap<String, MethodMapping>();

        // 新增类操作
        map.put("create", new MethodMapping("创建", BusinessType.ADD));
        map.put("add", new MethodMapping("新增", BusinessType.ADD));
        map.put("save", new MethodMapping("保存", BusinessType.ADD));
        map.put("register", new MethodMapping("注册", BusinessType.ADD));
        map.put("upload", new MethodMapping("上传", BusinessType.ADD));
        map.put("import", new MethodMapping("导入", BusinessType.ADD));

        // 修改类操作
        map.put("update", new MethodMapping("更新", BusinessType.UPDATE));
        map.put("edit", new MethodMapping("编辑", BusinessType.UPDATE));
        map.put("modify", new MethodMapping("修改", BusinessType.UPDATE));
        map.put("change", new MethodMapping("修改", BusinessType.UPDATE));
        map.put("reset", new MethodMapping("重置", BusinessType.UPDATE));
        map.put("mark", new MethodMapping("标记", BusinessType.UPDATE));
        map.put("approve", new MethodMapping("审批", BusinessType.UPDATE));
        map.put("reject", new MethodMapping("驳回", BusinessType.UPDATE));
        map.put("process", new MethodMapping("处理", BusinessType.UPDATE));
        map.put("handle", new MethodMapping("处理", BusinessType.UPDATE));
        map.put("bind", new MethodMapping("绑定", BusinessType.UPDATE));
        map.put("unbind", new MethodMapping("解绑", BusinessType.UPDATE));
        map.put("toggle", new MethodMapping("切换", BusinessType.UPDATE));
        map.put("audit", new MethodMapping("审核", BusinessType.UPDATE));
        map.put("enable", new MethodMapping("启用", BusinessType.UPDATE));
        map.put("disable", new MethodMapping("停用", BusinessType.UPDATE));
        map.put("ban", new MethodMapping("封禁", BusinessType.UPDATE));
        map.put("unban", new MethodMapping("解封", BusinessType.UPDATE));
        map.put("force", new MethodMapping("强制操作", BusinessType.UPDATE));
        map.put("unlock", new MethodMapping("解锁", BusinessType.UPDATE));
        map.put("recall", new MethodMapping("撤回", BusinessType.UPDATE));
        map.put("send", new MethodMapping("发送", BusinessType.UPDATE));
        map.put("typing", new MethodMapping("输入中", BusinessType.UPDATE));
        map.put("reply", new MethodMapping("回复", BusinessType.UPDATE));
        map.put("like", new MethodMapping("点赞", BusinessType.UPDATE));
        map.put("confirm", new MethodMapping("确认", BusinessType.UPDATE));
        map.put("submit", new MethodMapping("提交", BusinessType.UPDATE));
        map.put("assign", new MethodMapping("分配", BusinessType.UPDATE));
        map.put("publish", new MethodMapping("发布", BusinessType.UPDATE));
        map.put("batch", new MethodMapping("批量操作", BusinessType.UPDATE));
        map.put("sync", new MethodMapping("同步", BusinessType.UPDATE));
        map.put("refresh", new MethodMapping("刷新", BusinessType.UPDATE));
        map.put("clear", new MethodMapping("清空", BusinessType.UPDATE));
        map.put("recover", new MethodMapping("恢复", BusinessType.UPDATE));
        map.put("archive", new MethodMapping("归档", BusinessType.UPDATE));

        // 删除类操作
        map.put("delete", new MethodMapping("删除", BusinessType.DELETE));
        map.put("remove", new MethodMapping("删除", BusinessType.DELETE));
        map.put("cancel", new MethodMapping("取消", BusinessType.DELETE));

        // 登录类操作
        map.put("login", new MethodMapping("登录", BusinessType.LOGIN));
        map.put("logout", new MethodMapping("登出", BusinessType.LOGIN));

        // 本仓已有的动作动词 —— 漏配不会丢记录，但标题会回退成英文方法名（findMapping 无命中 → 原名 + OTHER）
        map.put("reindex", new MethodMapping("重建索引", BusinessType.UPDATE));
        map.put("payOrder", new MethodMapping("支付", BusinessType.UPDATE));
        map.put("paymentCallback", new MethodMapping("支付回调", BusinessType.UPDATE));
        map.put("shipOrder", new MethodMapping("发货", BusinessType.UPDATE));
        map.put("refund", new MethodMapping("退款", BusinessType.UPDATE));
        map.put("putOnline", new MethodMapping("上架", BusinessType.UPDATE));
        map.put("takeOffline", new MethodMapping("下架", BusinessType.UPDATE));
        map.put("close", new MethodMapping("关闭", BusinessType.UPDATE));
        map.put("smsLogin", new MethodMapping("短信登录", BusinessType.LOGIN));
        map.put("verify", new MethodMapping("校验", BusinessType.OTHER));
        map.put("recordSearch", new MethodMapping("记录搜索", BusinessType.OTHER));
        map.put("feedback", new MethodMapping("反馈", BusinessType.ADD));
        map.put("incrementViewCount", new MethodMapping("浏览计数", BusinessType.OTHER));
        map.put("autoListing", new MethodMapping("拍照识别上架", BusinessType.OTHER));
        map.put("chat", new MethodMapping("AI 对话", BusinessType.OTHER));
        map.put("stream", new MethodMapping("流式请求", BusinessType.OTHER));
        map.put("mock", new MethodMapping("模拟网关", BusinessType.OTHER));

        // 其它（仅记录标题，不归属具体业务类型）
        map.put("export", new MethodMapping("导出", BusinessType.OTHER));
        map.put("download", new MethodMapping("下载", BusinessType.OTHER));

        return map;
    }

    /**
     * 按长优先匹配方法名前缀，返回对应的操作映射。
     *
     * @param methodName Controller 方法名
     * @return 命中的映射；无命中（或入参为 null）时返回 empty，
     *     调用方回退为原始方法名 + {@link BusinessType#OTHER}
     */
    public Optional<MethodMapping> findMapping(String methodName) {
        if (methodName == null) {
            return Optional.empty();
        }
        String bestKey = null;
        int bestLen = 0;
        for (String key : methodMappings.keySet()) {
            if (methodName.startsWith(key) && key.length() > bestLen) {
                bestKey = key;
                bestLen = key.length();
            }
        }
        return bestKey == null ? Optional.empty() : Optional.of(methodMappings.get(bestKey));
    }

    /**
     * 方法名前缀对应的审计规则：操作标题与业务类型。
     *
     * @param title        操作标题，如 "创建"
     * @param businessType 业务类型，如 {@link BusinessType#ADD}
     */
    public record MethodMapping(String title, BusinessType businessType) {}
}

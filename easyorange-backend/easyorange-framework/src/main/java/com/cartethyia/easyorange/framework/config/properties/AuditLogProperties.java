package com.cartethyia.easyorange.framework.config.properties;

import com.cartethyia.easyorange.common.enums.BusinessType;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 审计日志配置属性
 * <p>
 * 用于控制审计日志的记录行为，包括是否启用、是否保存请求/响应数据等。
 * </p>
 * 配置示例：
 * <pre>{@code
 * audit:
 *   enabled: true
 *   save-request-data: false
 *   save-response-data: false
 *   retention-days: 180
 * }</pre>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "audit")
public class AuditLogProperties {

    /**
     * 是否启用审计日志记录
     * <p>
     * 默认为 true，设置为 false 时完全禁用日志记录功能
     * </p>
     */
    private boolean enabled = true;

    /**
     * 审计日志保留天数，超期由 AuditLogCleanupTask 每日清理。
     */
    @Min(1)
    private int retentionDays = 180;

    /**
     * 是否保存请求数据
     * <p>
     * 默认为 true，保存请求参数到 oper_param 字段
     * </p>
     */
    private boolean saveRequestData = true;

    /**
     * 是否保存响应数据
     * <p>
     * 默认为 false，设置为 true 时会保存 JSON 响应到 json_result 字段
     * 注意：开启此选项会增加数据库存储压力，生产环境建议保持关闭
     * </p>
     */
    private boolean saveResponseData = false;

    /**
     * 不记录日志的读操作方法名前缀列表
     * <p>
     * 方法名以这些前缀开头时跳过日志记录。
     * 默认覆盖常见地查询前缀。
     * </p>
     */
    private List<String> skipPrefixes =
            List.of("get", "query", "find", "list", "detail", "search", "count", "check", "exists", "stats", "my");

    /**
     * 请求参数中需要掩码的敏感字段名列表
     * <p>
     * 记录请求数据时，这些字段的值会被替换为 ******。
     * </p>
     */
    private List<String> sensitiveFields = List.of(
            "password",
            "confirmPassword",
            "oldPassword",
            "newPassword",
            "token",
            "secret",
            "secretKey",
            "accessToken",
            "refreshToken");

    /**
     * Controller 类名 → 中文模块名称映射
     * <p>
     * 用于从 Controller 类名推导审计日志的模块字段。
     * 按长优先匹配（如 "ProductReport" 优先于 "Product"）。
     * </p>
     */
    private Map<String, String> moduleNames = defaultModuleNames();

    /**
     * 方法名前缀 → 操作映射（标题 + 业务类型）
     * <p>
     * 审计日志推导的单一事实来源：从方法名前缀同时推导操作标题与业务类型，
     * 避免标题映射与类型映射两套表各自维护而漂移。按长优先匹配。
     * </p>
     */
    private Map<String, MethodMapping> methodMappings = defaultMethodMappings();

    private static Map<String, String> defaultModuleNames() {
        var map = new LinkedHashMap<String, String>();
        map.put("ProductReport", "商品举报");
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
        map.put("report", new MethodMapping("举报", BusinessType.UPDATE));
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

        // 其它（仅记录标题，不归属具体业务类型）
        map.put("export", new MethodMapping("导出", BusinessType.OTHER));
        map.put("download", new MethodMapping("下载", BusinessType.OTHER));

        return map;
    }

    public Map<String, String> getModuleNames() {
        return Map.copyOf(moduleNames);
    }

    public Map<String, MethodMapping> getMethodMappings() {
        return Map.copyOf(methodMappings);
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

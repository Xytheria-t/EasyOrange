package com.cartethyia.easyorange.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志配置属性
 * <p>
 * 用于控制审计日志的记录行为，包括是否启用、日志级别、
 * 是否保存请求/响应数据等。
 * </p>
 * 配置示例：
 * <pre>{@code
 * audit:
 *   enabled: true
 *   log-level: DETAILED
 *   save-request-data: false
 *   save-response-data: false
 * }</pre>
 */
@Data
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
     * 日志记录级别
     * <p>
     * - DISABLED: 不记录任何日志
     * - BASIC: 仅记录基本信息（操作时间、操作人、IP、操作类型）
     * - DETAILED: 记录完整信息（包含请求参数和响应数据）
     * </p>
     * 默认为 BASIC
     */
    private LogLevel logLevel = LogLevel.BASIC;

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
    private List<String> skipPrefixes = List.of(
            "get", "query", "find", "list", "detail",
            "search", "count", "check", "exists", "stats", "my"
    );

    /**
     * 请求参数中需要掩码的敏感字段名列表
     * <p>
     * 记录请求数据时，这些字段的值会被替换为 ******。
     * </p>
     */
    private List<String> sensitiveFields = List.of(
            "password", "confirmPassword", "oldPassword", "newPassword",
            "token", "secret", "secretKey", "accessToken", "refreshToken"
    );

    /**
     * Controller 类名 → 中文模块名称映射
     * <p>
     * 用于从 Controller 类名推导审计日志的模块字段。
     * 按长优先匹配（如 "ProductReport" 优先于 "Product"）。
     * </p>
     */
    private Map<String, String> moduleNames = defaultModuleNames();

    /**
     * 方法名前缀 → 操作标题映射
     * <p>
     * 用于从方法名推导审计日志的操作标题。
     * 按长优先匹配。
     * </p>
     */
    private Map<String, String> methodTitles = defaultMethodTitles();

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

    private static Map<String, String> defaultMethodTitles() {
        var map = new LinkedHashMap<String, String>();
        map.put("create", "创建");
        map.put("add", "新增");
        map.put("save", "保存");
        map.put("update", "更新");
        map.put("edit", "编辑");
        map.put("modify", "修改");
        map.put("delete", "删除");
        map.put("remove", "删除");
        map.put("import", "导入");
        map.put("export", "导出");
        map.put("approve", "审批");
        map.put("reject", "驳回");
        map.put("process", "处理");
        map.put("upload", "上传");
        map.put("download", "下载");
        map.put("login", "登录");
        map.put("logout", "登出");
        map.put("register", "注册");
        map.put("reset", "重置");
        map.put("change", "修改");
        map.put("cancel", "取消");
        map.put("confirm", "确认");
        map.put("submit", "提交");
        map.put("audit", "审核");
        map.put("assign", "分配");
        map.put("unbind", "解绑");
        map.put("bind", "绑定");
        map.put("mark", "标记");
        map.put("report", "举报");
        map.put("handle", "处理");
        map.put("send", "发送");
        map.put("publish", "发布");
        map.put("batch", "批量操作");
        map.put("sync", "同步");
        map.put("refresh", "刷新");
        map.put("clear", "清空");
        map.put("recover", "恢复");
        map.put("archive", "归档");
        return map;
    }

    public Map<String, String> getModuleNames() {
        return Map.copyOf(moduleNames);
    }

    public Map<String, String> getMethodTitles() {
        return Map.copyOf(methodTitles);
    }

    /**
     * 日志记录级别枚举
     */
    public enum LogLevel {
        /** 禁用日志 */
        DISABLED,
        /** 仅记录基本信息 */
        BASIC,
        /** 记录完整信息 */
        DETAILED
    }
}

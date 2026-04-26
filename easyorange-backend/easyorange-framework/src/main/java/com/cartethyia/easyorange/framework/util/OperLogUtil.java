package com.cartethyia.easyorange.framework.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志工具类
 * 提供方法名推导操作标题、模块名称映射、字符串截断等功能
 *
 * @author cartethyia
 */
public final class OperLogUtil {

    private OperLogUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static final List<Map.Entry<String, String>> METHOD_PREFIX_LIST;
    private static final List<Map.Entry<String, String>> CONTROLLER_MODULE_LIST;

    static {
        Map<String, String> methodPrefixMap = new HashMap<>();
        methodPrefixMap.put("create", "创建");
        methodPrefixMap.put("add", "新增");
        methodPrefixMap.put("save", "保存");
        methodPrefixMap.put("update", "更新");
        methodPrefixMap.put("edit", "编辑");
        methodPrefixMap.put("modify", "修改");
        methodPrefixMap.put("delete", "删除");
        methodPrefixMap.put("remove", "删除");
        methodPrefixMap.put("import", "导入");
        methodPrefixMap.put("export", "导出");
        methodPrefixMap.put("approve", "审批");
        methodPrefixMap.put("reject", "驳回");
        methodPrefixMap.put("process", "处理");
        methodPrefixMap.put("upload", "上传");
        methodPrefixMap.put("download", "下载");
        methodPrefixMap.put("login", "登录");
        methodPrefixMap.put("logout", "登出");
        methodPrefixMap.put("register", "注册");
        methodPrefixMap.put("reset", "重置");
        methodPrefixMap.put("change", "修改");
        methodPrefixMap.put("cancel", "取消");
        methodPrefixMap.put("confirm", "确认");
        methodPrefixMap.put("submit", "提交");
        methodPrefixMap.put("audit", "审核");
        methodPrefixMap.put("assign", "分配");
        methodPrefixMap.put("unbind", "解绑");
        methodPrefixMap.put("bind", "绑定");
        methodPrefixMap.put("mark", "标记");
        methodPrefixMap.put("report", "举报");
        methodPrefixMap.put("handle", "处理");
        methodPrefixMap.put("send", "发送");
        methodPrefixMap.put("publish", "发布");
        methodPrefixMap.put("batch", "批量操作");
        methodPrefixMap.put("sync", "同步");
        methodPrefixMap.put("refresh", "刷新");
        methodPrefixMap.put("clear", "清空");
        methodPrefixMap.put("recover", "恢复");
        methodPrefixMap.put("archive", "归档");

        METHOD_PREFIX_LIST = methodPrefixMap.entrySet().stream()
                .sorted((a, b) -> b.getKey().length() - a.getKey().length())
                .toList();

        Map<String, String> controllerModuleMap = new HashMap<>();
        controllerModuleMap.put("ProductReport", "商品举报");
        controllerModuleMap.put("Product", "商品管理");
        controllerModuleMap.put("User", "用户管理");
        controllerModuleMap.put("Auth", "认证管理");
        controllerModuleMap.put("OrderCommand", "订单管理");
        controllerModuleMap.put("OrderQuery", "订单管理");
        controllerModuleMap.put("Order", "订单管理");
        controllerModuleMap.put("MessageCommand", "消息管理");
        controllerModuleMap.put("MessageQuery", "消息管理");
        controllerModuleMap.put("Message", "消息管理");
        controllerModuleMap.put("PaymentCommand", "支付管理");
        controllerModuleMap.put("PaymentQuery", "支付管理");
        controllerModuleMap.put("Payment", "支付管理");
        controllerModuleMap.put("File", "文件管理");
        controllerModuleMap.put("Search", "搜索管理");
        controllerModuleMap.put("Health", "系统健康");
        controllerModuleMap.put("Menu", "菜单管理");
        controllerModuleMap.put("Role", "角色管理");
        controllerModuleMap.put("Dept", "部门管理");
        controllerModuleMap.put("Dict", "字典管理");
        controllerModuleMap.put("Config", "配置管理");
        controllerModuleMap.put("Notice", "通知公告");
        controllerModuleMap.put("LoginLog", "登录日志");
        controllerModuleMap.put("OperLog", "操作日志");

        CONTROLLER_MODULE_LIST = controllerModuleMap.entrySet().stream()
                .sorted((a, b) -> b.getKey().length() - a.getKey().length())
                .toList();
    }

    /**
     * 从方法名推导操作标题
     * <p>
     * createProduct -&gt; "创建"
     * updatePassword -&gt; "更新"
     * </p>
     *
     * @param methodName 方法名
     * @return 操作标题
     */
    public static String deriveOperationTitle(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return methodName;
        }

        for (Map.Entry<String, String> entry : METHOD_PREFIX_LIST) {
            if (methodName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return methodName;
    }

    /**
     * 从 Controller 类名推导模块名称
     * <p>
     * ProductController -&gt; "商品管理"
     * UserController -&gt; "用户管理"
     * </p>
     *
     * @param controllerClassName Controller 类名
     * @return 模块名称
     */
    public static String deriveModuleName(String controllerClassName) {
        if (controllerClassName == null || controllerClassName.isEmpty()) {
            return controllerClassName;
        }

        String name = controllerClassName.replace("Controller", "")
                .replace("Command", "")
                .replace("Query", "");

        for (Map.Entry<String, String> entry : CONTROLLER_MODULE_LIST) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return name;
    }

    private static final String TRUNCATE_SUFFIX = "...(已截断)";

    /**
     * 截断字符串到指定长度
     *
     * @param str 原字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must be non-negative, actual: " + maxLength);
        }
        if (str.length() <= maxLength) {
            return str;
        }

        int truncateLength = maxLength - TRUNCATE_SUFFIX.length();
        if (truncateLength <= 0) {
            return str.substring(0, maxLength);
        }
        return str.substring(0, truncateLength) + TRUNCATE_SUFFIX;
    }
}

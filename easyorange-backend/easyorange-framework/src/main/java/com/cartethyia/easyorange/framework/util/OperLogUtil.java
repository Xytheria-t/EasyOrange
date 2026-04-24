package com.cartethyia.easyorange.framework.util;

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

    /**
     * 方法名前缀到操作标题的映射
     */
    private static final String[][] METHOD_PREFIX_MAPPING = {
            {"create", "创建"}, {"add", "新增"}, {"save", "保存"},
            {"update", "更新"}, {"edit", "编辑"}, {"modify", "修改"},
            {"delete", "删除"}, {"remove", "删除"},
            {"import", "导入"}, {"export", "导出"},
            {"approve", "审批"}, {"reject", "驳回"},
            {"process", "处理"}, {"upload", "上传"},
            {"download", "下载"}, {"login", "登录"},
            {"logout", "登出"}, {"register", "注册"},
            {"reset", "重置"}, {"change", "修改"},
            {"cancel", "取消"}, {"confirm", "确认"},
            {"submit", "提交"}, {"audit", "审核"},
            {"assign", "分配"}, {"unbind", "解绑"},
            {"bind", "绑定"}, {"mark", "标记"},
            {"report", "举报"}, {"handle", "处理"},
            {"send", "发送"}, {"publish", "发布"},
            {"batch", "批量操作"}, {"sync", "同步"},
            {"refresh", "刷新"}, {"clear", "清空"},
            {"recover", "恢复"}, {"archive", "归档"}
    };

    /**
     * Controller 类名到模块名称的映射
     */
    private static final String[][] CONTROLLER_MODULE_MAPPING = {
            {"Product", "商品管理"}, {"ProductReport", "商品举报"},
            {"User", "用户管理"}, {"Auth", "认证管理"},
            {"Order", "订单管理"}, {"OrderCommand", "订单管理"},
            {"OrderQuery", "订单管理"},
            {"Message", "消息管理"}, {"MessageCommand", "消息管理"},
            {"MessageQuery", "消息管理"},
            {"Payment", "支付管理"}, {"PaymentCommand", "支付管理"},
            {"PaymentQuery", "支付管理"},
            {"File", "文件管理"},
            {"Search", "搜索管理"},
            {"Health", "系统健康"},
            {"Menu", "菜单管理"}, {"Role", "角色管理"},
            {"Dept", "部门管理"}, {"Dict", "字典管理"},
            {"Config", "配置管理"}, {"Notice", "通知公告"},
            {"LoginLog", "登录日志"}, {"OperLog", "操作日志"}
    };

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

        for (String[] mapping : METHOD_PREFIX_MAPPING) {
            if (methodName.startsWith(mapping[0])) {
                return mapping[1];
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

        for (String[] mapping : CONTROLLER_MODULE_MAPPING) {
            if (name.contains(mapping[0])) {
                return mapping[1];
            }
        }

        return name;
    }

    /**
     * 截断字符串到指定长度
     *
     * @param str 原字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...(已截断)";
    }
}

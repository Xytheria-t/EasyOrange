package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 搜索工具注册表 — Spring 自动收集所有 {@link SearchTool} bean，按 {@link SearchTool#name()} 取用。
 * <p>
 * 工具名冲突（两个工具同名）在构造期直接抛错，避免运行期静默覆盖。
 * <p>
 * 编排器（{@code AiSearchEnhancerAdapter}）取工具时引用各工具自己的 {@code NAME} 常量，
 * 不在调用侧重写字符串字面量：名字两处各写一遍的话，改了工具名会让注册表查不到、
 * 在编排器的 catch 里被吞成「本次无增强」——静默降级比启动失败难查得多。
 */
@Component
public class SearchToolRegistry {

    private final Map<String, SearchTool<?>> tools;

    public SearchToolRegistry(List<SearchTool<?>> tools) {
        this.tools = tools.stream()
                .collect(Collectors.toUnmodifiableMap(SearchTool::name, tool -> tool, (existing, duplicate) -> {
                    throw new IllegalArgumentException("Duplicate search tool name: " + existing.name());
                }));
    }

    public SearchTool<?> get(String name) {
        SearchTool<?> tool = tools.get(name);
        if (tool == null) {
            throw new NoSuchElementException("Unknown search tool: " + name);
        }
        return tool;
    }
}

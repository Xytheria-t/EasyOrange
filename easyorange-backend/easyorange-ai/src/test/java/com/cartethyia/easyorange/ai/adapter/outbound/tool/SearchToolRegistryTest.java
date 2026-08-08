package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SearchToolRegistry -> 测试")
class SearchToolRegistryTest {

    private static final SearchTool<String> DUMMY =
            new SearchTool<String>() {
                @Override
                public String name() {
                    return "dummy";
                }

                @Override
                public String description() {
                    return "dummy tool";
                }

                @Override
                public SearchToolKind kind() {
                    return SearchToolKind.RULE;
                }

                @Override
                public CompletableFuture<String> run(SearchToolContext context) {
                    return CompletableFuture.completedFuture("ok");
                }
            };

    private static final SearchToolContext CTX =
            new SearchToolContext("找电脑", List.<ProductReadModel>of(), "");

    @Test
    @DisplayName("自动收集所有工具 -> all() 返回全部")
    void registry_collectsAllTools() {
        var registry = new SearchToolRegistry(List.of(DUMMY));

        assertThat(registry.all()).hasSize(1).containsExactly(DUMMY);
        assertThat(registry.contains("dummy")).isTrue();
        assertThat(registry.contains("missing")).isFalse();
    }

    @Test
    @DisplayName("get 按名字取工具 -> 执行正常")
    void registry_getByName() {
        var registry = new SearchToolRegistry(List.of(DUMMY));

        SearchTool<?> tool = registry.get("dummy");

        assertThat(tool).isSameAs(DUMMY);
        assertThat(tool.run(CTX).join()).isEqualTo("ok");
    }

    @Test
    @DisplayName("重复工具名 -> 构造期抛错，不做运行期静默覆盖")
    void registry_duplicateNameThrows() {
        var duplicate = new SearchTool<String>() {
            @Override
            public String name() {
                return "dummy";
            }

            @Override
            public String description() {
                return "duplicate";
            }

            @Override
            public SearchToolKind kind() {
                return SearchToolKind.RULE;
            }

            @Override
            public CompletableFuture<String> run(SearchToolContext context) {
                return CompletableFuture.completedFuture("dup");
            }
        };

        assertThatThrownBy(() -> new SearchToolRegistry(List.of(DUMMY, duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate search tool name: dummy");
    }

    @Test
    @DisplayName("get 未知工具名 -> 抛 NoSuchElementException")
    void registry_getUnknownThrows() {
        var registry = new SearchToolRegistry(List.of(DUMMY));

        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Unknown search tool: unknown");
    }

    @Test
    @DisplayName("工具元数据完整性 -> name/description/kind 非空")
    void tool_metadataComplete() {
        assertThat(DUMMY.name()).isNotBlank();
        assertThat(DUMMY.description()).isNotBlank();
        assertThat(DUMMY.kind()).isNotNull();
        assertThat(DUMMY.kind()).isEqualTo(SearchToolKind.RULE);
    }

    @Test
    @DisplayName("空注册表 -> all() 为空集合，get 抛错")
    void registry_emptyAllowed() {
        var registry = new SearchToolRegistry(List.of());

        assertThat(registry.all()).isEmpty();
        assertThatThrownBy(() -> registry.get("anything")).isInstanceOf(NoSuchElementException.class);
    }
}

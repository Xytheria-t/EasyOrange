package com.cartethyia.easyorange.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Embedding 维度一致性测试 — 守住「维度写三处」这个静默劣化坑。
 *
 * <p>向量维度在仓库里出现三份：application.yaml 的 {@code easyorange.ai.embedding.dimensions}、
 * 两个 ES 映射的 {@code dense_vector.dims}、以及 {@link AiProperties} 构造器为「属性源整段缺失」
 * 留的兜底值。三处失同步<b>不报错、不打日志</b>：单测用 PropertyBindings 裸绑（不加载 yaml）会静默吃
 * Java 兜底的旧值，起服后 yaml 的新值覆盖它，而 ES 索引是按第三份建的 —— 症状是「kNN 查询维度不匹配」
 * 或「能召回但结果很烂」，排查成本极高（当前 1024，与 DashScope text-embedding-v3 对齐）。
 *
 * <p>换 embedding 模型必须三处同改并重建 ES 索引，本测试就是那个「忘了改某一处」的哨兵。
 */
@DisplayName("Embedding 维度一致性")
class EmbeddingDimensionsConsistencyTest {

    private static final String DIMENSIONS_PROPERTY = "easyorange.ai.embedding.dimensions";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @DisplayName("application.yaml 的 dimensions 与两个 ES 映射的 dense_vector.dims 一致")
    void yamlMatchesElasticsearchMappings() throws Exception {
        int yamlDims = dimensionsFromYaml();

        assertThat(dimsOfVectorField("elasticsearch/product-mapping.json", "nameEmbedding"))
                .as(
                        "application.yaml dimensions=%d 与 product-mapping.json 不一致 —— " + "kNN 查询会维度不匹配，需同步改映射并重建索引",
                        yamlDims)
                .isEqualTo(yamlDims);
        assertThat(dimsOfVectorField("elasticsearch/knowledge-mapping.json", "embedding"))
                .as(
                        "application.yaml dimensions=%d 与 knowledge-mapping.json 不一致 —— "
                                + "知识库 kNN 检索会维度不匹配，需同步改映射并重建索引",
                        yamlDims)
                .isEqualTo(yamlDims);
    }

    @Test
    @DisplayName("AiProperties 构造器兜底的 dimensions 与 application.yaml 一致")
    void constructorFallbackMatchesYaml() throws Exception {
        // 构造器兜底只在属性源整段缺失时生效（单测裸绑场景），但它会与 yaml 静默分叉：
        // 本地测试全绿、起服行为不同。这条断言把「本地绿 ≠ 线上绿」这个失效模式关掉。
        var fallback = new AiProperties(null, null, null, null, null, null, null, null, null, null);

        assertThat(fallback.embedding().dimensions())
                .as(
                        "AiProperties 兜底 dimensions 与 application.yaml 的 %s 不一致 —— " + "单测裸绑时静默吃兜底旧值，表现为「测试全绿、起服行为不同」",
                        DIMENSIONS_PROPERTY)
                .isEqualTo(dimensionsFromYaml());
    }

    private static int dimensionsFromYaml() throws Exception {
        try (InputStream yaml = resource("application.yaml")) {
            // application.yaml 是多文档（--- 分隔 profile），Yaml#load 只收单文档会抛
            // "expected a single document"；easyorange.ai 段落在首个文档里。loadAll 返回 Iterable 无 stream()。
            Object root = null;
            for (Object document : new Yaml().loadAll(yaml)) {
                if (document != null) {
                    root = document;
                    break;
                }
            }
            assertThat(root).as("application.yaml 首个文档解析为空").isNotNull();
            Object dimensions = dig(root, "easyorange", "ai", "embedding", "dimensions");
            assertThat(dimensions)
                    .as("%s 必须显式配值 —— 它是两个 ES 映射的对齐基准，不能靠 Spring 占位符默认值兜", DIMENSIONS_PROPERTY)
                    .isNotNull();
            return Integer.parseInt(String.valueOf(dimensions));
        }
    }

    /** 逐层取嵌套 map 的键值，缺失即 null（不抛 NPE，让断言去报「哪里缺了」而不是一个无信息的栈）。 */
    private static Object dig(Object root, String... keys) {
        Object current = root;
        for (String key : keys) {
            if (!(current instanceof java.util.Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    /**
     * 取 mapping 里 dense_vector 字段的 dims —— 只认 dense_vector 字段，避免改到无关数值字段时误报。
     * 两个 mapping 的顶层就是 {@code properties}（不带 {@code mappings} 包装层）。
     */
    private static int dimsOfVectorField(String resourcePath, String field) throws Exception {
        try (InputStream mapping = resource(resourcePath)) {
            JsonNode properties = JSON.readTree(mapping).path("properties").path(field);
            assertThat(properties.path("type").asText())
                    .as("%s 的 %s 必须是 dense_vector，否则说明映射结构变了，本测试需跟着改", resourcePath, field)
                    .isEqualTo("dense_vector");
            return properties.path("dims").asInt();
        }
    }

    private static InputStream resource(String path) {
        var stream = EmbeddingDimensionsConsistencyTest.class.getClassLoader().getResourceAsStream(path);
        assertThat(stream).as("测试资源缺失：%s", path).isNotNull();
        return stream;
    }
}

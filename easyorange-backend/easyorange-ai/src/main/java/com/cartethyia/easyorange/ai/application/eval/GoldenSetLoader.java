package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.application.chat.ChatTools;
import com.cartethyia.easyorange.ai.application.listing.ListingTools;
import com.cartethyia.easyorange.ai.domain.model.GoldenSet;
import com.cartethyia.easyorange.ai.domain.model.GoldenSetCase;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * 金标准评测集加载器 — 从 classpath 读取 {@code eval/golden-set.yaml} 与 {@code eval/baselines.yaml}。
 * 评测集是 YAML（版本化、可评审 diff）不落库，与 Prompt YAML 同一套「配置即代码」思路。
 * <p>
 * <b>加载即校验</b>：scope 只认 {@code chat} / {@code retrieval} / {@code listing}，chat 必须有参考回答、
 * retrieval 必须有 gold_doc_ids 且不得标路由、listing 必须标路由且不得带参考回答 / gold 文档（发布用例是
 * 路由专用，串了指标照样算得出来但悄悄失真，这类错误必须在加载期炸掉）。{@code expected_tools} 的取值
 * 同样在加载期按 scope 对应工具面的工具名强校验。
 */
@Slf4j
@Component
public class GoldenSetLoader {

    public static final String GOLDEN_SET_PATH = "eval/golden-set.yaml";
    public static final String BASELINES_PATH = "eval/baselines.yaml";

    /** 生成质量用例的 scope 值。 */
    public static final String SCOPE_CHAT = "chat";

    /** 检索质量用例的 scope 值。 */
    public static final String SCOPE_RETRIEVAL = "retrieval";

    /** 发布链路路由用例的 scope 值（多步工具循环，只跑循环不生成）。 */
    public static final String SCOPE_LISTING = "listing";

    public GoldenSet load() {
        try (var in = new ClassPathResource(GOLDEN_SET_PATH).getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            if (root == null || root.get("cases") == null) {
                return new GoldenSet(List.of());
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawCases = (List<Map<String, Object>>) root.get("cases");
            var cases = rawCases.stream().map(this::toCase).toList();
            cases.forEach(GoldenSetLoader::validate);
            return new GoldenSet(cases);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load golden set: " + GOLDEN_SET_PATH, e);
        }
    }

    /** 用例自洽性校验：scope 合法 + 该 scope 必需的字段齐全 + 期望工具名合法。包可见静态便于单测直接覆盖。 */
    static void validate(GoldenSetCase testCase) {
        String id = testCase.id() == null ? "(缺少 id)" : testCase.id();
        if (SCOPE_CHAT.equals(testCase.scope())) {
            if (testCase.referenceAnswer() == null || testCase.referenceAnswer().isBlank()) {
                throw new IllegalStateException("金标准用例 " + id + " 是 chat 用例，必须提供 reference_answer");
            }
            validateExpectedTools(id, testCase.expectedTools(), ChatTools.TOOL_NAMES, "chat");
            return;
        }
        if (SCOPE_RETRIEVAL.equals(testCase.scope())) {
            if (testCase.goldDocIds().isEmpty()) {
                throw new IllegalStateException("金标准用例 " + id + " 是 retrieval 用例，必须提供 gold_doc_ids");
            }
            // retrieval 用例不跑工具循环，标注期望工具只会永远评不出命中 —— 加载期拒掉而不是当空标注
            if (!testCase.expectedTools().isEmpty()) {
                throw new IllegalStateException("金标准用例 " + id + " 是 retrieval 用例，不跑工具循环，不能标 expected_tools");
            }
            return;
        }
        if (SCOPE_LISTING.equals(testCase.scope())) {
            // listing 是路由专用 scope：expected_tools 就是它的全部产出，标空等于白加；参考回答与 gold 文档
            // 都不属于它（前者没有 Judge 评分线，后者会把发布用例悄悄算进检索分母）
            if (testCase.expectedTools().isEmpty()) {
                throw new IllegalStateException("金标准用例 " + id + " 是 listing 用例，必须标 expected_tools（多步路径）");
            }
            if (testCase.referenceAnswer() != null
                    && !testCase.referenceAnswer().isBlank()) {
                throw new IllegalStateException("金标准用例 " + id + " 是 listing 用例，只评路由不评生成，不能标 reference_answer");
            }
            if (!testCase.goldDocIds().isEmpty()) {
                throw new IllegalStateException("金标准用例 " + id + " 是 listing 用例，不进检索分母，不能标 gold_doc_ids");
            }
            validateExpectedTools(id, testCase.expectedTools(), ListingTools.TOOL_NAMES, "listing");
            return;
        }
        throw new IllegalStateException(
                "金标准用例 " + id + " 的 scope 非法：" + testCase.scope() + "（只允许 chat / retrieval / listing）");
    }

    /** 期望工具名必须取自对应 scope 的工具面 —— 工具改名后 yaml 里的旧名字会在加载期炸，而不是静默评成路由走错。 */
    private static void validateExpectedTools(
            String id, List<String> expectedTools, Set<String> validTools, String scope) {
        for (String tool : expectedTools) {
            if (!validTools.contains(tool)) {
                throw new IllegalStateException(
                        "金标准用例 %s 的 expected_tools 含未知工具 %s（scope=%s 合法值：%s）".formatted(id, tool, scope, validTools));
            }
        }
    }

    /**
     * 评估门禁阈值（baselines.yaml）— 分数基线、容忍度、覆盖率下限、hit@5 与路由准确率下限。
     * 键缺失即抛异常：门禁阈值静默回落成内置默认值，等于门禁悄悄放松（改了 yaml 的键名却照旧跑绿）。
     */
    public EvalBaselines loadBaselines() {
        try (var in = new ClassPathResource(BASELINES_PATH).getInputStream()) {
            Map<String, Object> raw = new Yaml().load(in);
            return new EvalBaselines(
                    new EvalBaselines.Generation(
                            required(raw, "generation", "score-baseline"),
                            required(raw, "generation", "score-tolerance"),
                            required(raw, "generation", "min-coverage")),
                    new EvalBaselines.Retrieval(required(raw, "retrieval", "min-hit-at-5")),
                    new EvalBaselines.Routing(
                            required(raw, "routing", "min-accuracy"),
                            required(raw, "routing", "listing-min-accuracy")));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load baselines: " + BASELINES_PATH, e);
        }
    }

    private static double required(Map<String, Object> root, String scope, String key) {
        Object section = root == null ? null : root.get(scope);
        Object value = section instanceof Map<?, ?> map ? map.get(key) : null;
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("baselines.yaml 缺少 " + scope + "." + key);
        }
        return number.doubleValue();
    }

    private GoldenSetCase toCase(Map<String, Object> raw) {
        return new GoldenSetCase(
                (String) raw.get("id"),
                (String) raw.get("scope"),
                (String) raw.get("question"),
                (String) raw.get("reference_answer"),
                stringList(raw.get("gold_doc_ids")),
                stringList(raw.get("expected_tools")));
    }

    private static List<String> stringList(Object value) {
        return value == null
                ? List.of()
                : ((List<?>) value).stream().map(String::valueOf).toList();
    }
}

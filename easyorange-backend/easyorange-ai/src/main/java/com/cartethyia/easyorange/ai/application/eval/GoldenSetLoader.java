package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.domain.model.GoldenSet;
import com.cartethyia.easyorange.ai.domain.model.GoldenSetCase;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * 金标准评测集加载器 — 从 classpath 读取 {@code eval/golden-set.yaml} 与 {@code eval/baselines.yaml}。
 * <p>
 * 评测集是 YAML（版本化、可评审 diff），不落库 — 与 Prompt YAML 同一套「配置即代码」思路。
 * <p>
 * <b>加载即校验</b>：scope 只认 {@code chat} / {@code retrieval}，且 chat 必须有参考回答、
 * retrieval 必须有 gold_doc_ids。此前 scope 全写成 chat，两类用例在过滤时互相串门
 * （检索用例被当生成用例打分、生成用例被算进 hit@5），指标照样算得出来，问题不会暴露 ——
 * 这类错误必须在加载期炸掉，而不是等指标悄悄失真。
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

    /**
     * 用例自洽性校验：scope 合法 + 该 scope 必需的字段齐全。包可见静态便于单测直接覆盖。
     */
    static void validate(GoldenSetCase testCase) {
        String id = testCase.id() == null ? "(缺少 id)" : testCase.id();
        if (SCOPE_CHAT.equals(testCase.scope())) {
            if (testCase.referenceAnswer() == null || testCase.referenceAnswer().isBlank()) {
                throw new IllegalStateException("金标准用例 " + id + " 是 chat 用例，必须提供 reference_answer");
            }
            return;
        }
        if (SCOPE_RETRIEVAL.equals(testCase.scope())) {
            if (testCase.goldDocIds().isEmpty()) {
                throw new IllegalStateException("金标准用例 " + id + " 是 retrieval 用例，必须提供 gold_doc_ids");
            }
            return;
        }
        throw new IllegalStateException("金标准用例 " + id + " 的 scope 非法：" + testCase.scope() + "（只允许 chat / retrieval）");
    }

    /**
     * 评估门禁阈值（baselines.yaml）— 分数基线、容忍度、覆盖率下限、hit@5 下限。
     * <p>
     * 键缺失即抛异常：门禁阈值静默回落成内置默认值，等于门禁悄悄放松（改了 yaml 的键名却照旧跑绿），
     * 与其那样不如加载期炸掉。
     */
    public EvalBaselines loadBaselines() {
        try (var in = new ClassPathResource(BASELINES_PATH).getInputStream()) {
            Map<String, Object> raw = new Yaml().load(in);
            return new EvalBaselines(
                    new EvalBaselines.Generation(
                            required(raw, "generation", "score-baseline"),
                            required(raw, "generation", "score-tolerance"),
                            required(raw, "generation", "min-coverage")),
                    new EvalBaselines.Retrieval(required(raw, "retrieval", "min-hit-at-5")));
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
        Object gold = raw.get("gold_doc_ids");
        List<String> goldIds = gold == null
                ? List.of()
                : ((List<?>) gold).stream().map(String::valueOf).toList();
        return new GoldenSetCase(
                (String) raw.get("id"),
                (String) raw.get("scope"),
                (String) raw.get("question"),
                (String) raw.get("reference_answer"),
                goldIds);
    }
}

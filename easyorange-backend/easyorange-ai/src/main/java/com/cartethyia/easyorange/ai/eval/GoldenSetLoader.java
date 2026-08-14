package com.cartethyia.easyorange.ai.eval;

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
 */
@Slf4j
@Component
public class GoldenSetLoader {

    public static final String GOLDEN_SET_PATH = "eval/golden-set.yaml";
    public static final String BASELINES_PATH = "eval/baselines.yaml";

    public GoldenSet load() {
        try (var in = new ClassPathResource(GOLDEN_SET_PATH).getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            if (root == null || root.get("cases") == null) {
                return new GoldenSet(List.of());
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawCases = (List<Map<String, Object>>) root.get("cases");
            return new GoldenSet(rawCases.stream().map(this::toCase).toList());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load golden set: " + GOLDEN_SET_PATH, e);
        }
    }

    /**
     * 每场景质量基线（baselines.yaml）— 分数门禁的对比基准。
     */
    public Map<String, Double> loadBaselines() {
        try (var in = new ClassPathResource(BASELINES_PATH).getInputStream()) {
            Map<String, Object> raw = new Yaml().load(in);
            if (raw == null) {
                return Map.of();
            }
            return raw.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey, e -> ((Number) e.getValue()).doubleValue()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load baselines: " + BASELINES_PATH, e);
        }
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

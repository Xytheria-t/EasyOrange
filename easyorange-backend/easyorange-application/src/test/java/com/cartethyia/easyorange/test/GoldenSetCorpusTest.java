package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.application.eval.GoldenSetLoader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 评测集 ↔ 种子语料一致性（不需要 AI key，每次 CI 都跑）。
 * <p>
 * 金标准集的 gold_doc_ids 与知识库种子文档 R__seed_knowledge_docs.sql 是两份独立文件，
 * 靠人工保持一致：ID 写错、语料被删、语料规模退回与 topK 同量级，都会让 hit@5 变成
 * 「测不出来」或「必然满分」—— 指标照样有数字，但已经不代表检索质量。
 * 这里把这些约束断言化，让这类错误在单元测试阶段就暴露，而不是等评测回归时误判成模型问题。
 */
@DisplayName("评测集与种子语料一致性 -> 测试")
class GoldenSetCorpusTest {

    /** 与 GoldenSetEvaluator.RETRIEVAL_TOP_K / ChatTools.RETRIEVAL_TOP_K 保持一致（两者均为 5）。 */
    private static final int RETRIEVAL_TOP_K = 5;

    /**
     * 语料分块数相对 topK 的最小倍数：低于这个倍数时 topK 覆盖了语料的大部分，
     * hit@5 会退化成接近必然（此前 5 篇文档 + topK=5，hit@5 恒为 100% 就是这么来的）。
     */
    private static final int MIN_CORPUS_TO_TOPK_RATIO = 3;

    private static final Pattern DOC_ROW = Pattern.compile("\\((?:\\s*)'(kb-\\d+)'");

    @Test
    @DisplayName("检索用例引用的 gold_doc_ids 都真实存在于种子语料")
    void goldDocIdsExistInSeedCorpus() {
        Set<String> corpus = seedDocIds();

        var missing = new GoldenSetLoader()
                .load().cases().stream()
                        .flatMap(c -> c.goldDocIds().stream())
                        .distinct()
                        .filter(id -> !corpus.contains(id))
                        .toList();

        assertThat(missing).as("评测集引用了种子语料里不存在的文档，这些用例永远不可能命中：%s", missing).isEmpty();
    }

    @Test
    @DisplayName("语料规模显著大于 topK（否则 hit@5 失去判别力）")
    void corpusIsLargeEnoughForTopK() {
        int corpusSize = seedDocIds().size();

        assertThat(corpusSize)
                .as("语料 %d 篇，RETRIEVAL_TOP_K=%d：检索几乎必然覆盖全库，hit@5 不再有判别力", corpusSize, RETRIEVAL_TOP_K)
                .isGreaterThanOrEqualTo(RETRIEVAL_TOP_K * MIN_CORPUS_TO_TOPK_RATIO);
    }

    @Test
    @DisplayName("语料含多块长文档 —— 分块质量被评测覆盖（全部单块等于 chunking 从未进过评测范围）")
    void seedCorpusIncludesMultiChunkDocs() {
        // 分块阈值 500 字（KnowledgeIngestionAppService.CHUNK_SIZE）：正文超阈值会切多块。
        // 此前语料全部单块，长文档切完还能不能检索全凭手感；两条多块用例（retr-025/026）补上这个盲区。
        assertThat(longestContentLength())
                .as("种子语料应至少含一篇超分块阈值的长文档（chunking 行为需要有评测用例盯着）")
                .isGreaterThanOrEqualTo(500);
    }

    @Test
    @DisplayName("多块长文档被金标准集引用（切完检索不到时评测会红，而不是静默劣化）")
    void multiChunkDocsAreReferencedByGoldenSet() {
        Set<String> goldenDocIds = new GoldenSetLoader()
                .load().cases().stream()
                        .flatMap(c -> c.goldDocIds().stream())
                        .collect(java.util.stream.Collectors.toSet());

        assertThat(goldenDocIds.stream().anyMatch(id -> longestLiteralAfter(id) >= 500))
                .as("金标准集应引用至少一篇多块长文档，否则分块质量没有评测路径")
                .isTrue();
    }

    /** docId 所在行内最长字符串字面量长度 —— 正文是该行最长的字面量（标题 / 来源都是短语）。 */
    private static int longestLiteralAfter(String docId) {
        String sql = seedSql();
        int from = sql.indexOf("'" + docId + "'");
        assertThat(from).as("种子语料里找不到文档 %s", docId).isGreaterThanOrEqualTo(0);
        int lineEnd = sql.indexOf('\n', from);
        String row = lineEnd < 0 ? sql.substring(from) : sql.substring(from, lineEnd);
        Pattern literal = Pattern.compile("'((?:[^'\\\\]|\\\\.)*)'");
        Matcher matcher = literal.matcher(row);
        int longest = 0;
        while (matcher.find()) {
            longest = Math.max(longest, matcher.group(1).replace("\\n", "\n").length());
        }
        return longest;
    }

    private static Set<String> seedDocIds() {
        Matcher matcher = DOC_ROW.matcher(seedSql());
        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        assertThat(ids).as("种子迁移里应能解析出文档 ID").isNotEmpty();
        return ids;
    }

    /** 粗略取最长的单引号字符串长度，作为「正文是否超分块阈值」的上界估计。 */
    private static int longestContentLength() {
        Pattern literal = Pattern.compile("'((?:[^'\\\\]|\\\\.)*)'");
        Matcher matcher = literal.matcher(seedSql());
        int longest = 0;
        while (matcher.find()) {
            longest = Math.max(longest, matcher.group(1).replace("\\n", "\n").length());
        }
        return longest;
    }

    private static String seedSql() {
        try (InputStream in = GoldenSetCorpusTest.class
                .getClassLoader()
                .getResourceAsStream("db/migration/R__seed_knowledge_docs.sql")) {
            assertThat(in).as("种子迁移文件应在 classpath 上").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("读取种子迁移失败", e);
        }
    }
}

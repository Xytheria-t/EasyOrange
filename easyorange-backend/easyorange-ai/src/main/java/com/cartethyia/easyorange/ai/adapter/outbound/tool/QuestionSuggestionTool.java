package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

/**
 * 建议问题工具 — 由关键词 + 命中分类 + 价格下限派生模板问题，零 LLM 调用。
 * <p>
 * 这一路的产出本来就是模板化的追问，交给模型生成等于为「换个说法」付一次供应商调用，
 * 还得跟着吃超时缺席与降级重算。改为确定性派生后，输出可预期、不再有失败态。
 */
@Component
public class QuestionSuggestionTool implements SearchTool<List<String>> {

    /** 工具名 —— 编排器按它取用，故此处是唯一定义处（见 SearchToolRegistry）。 */
    public static final String NAME = "question_suggestion";

    /** 自然语言关键词可能几十字，原样回显进问题会很长。 */
    private static final int MAX_KEYWORD_CHARS = 12;

    private static final int MAX_QUESTIONS = 3;
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CompletableFuture<List<String>> run(SearchToolContext context) {
        return CompletableFuture.supplyAsync(() -> suggest(context), VIRTUAL);
    }

    private static List<String> suggest(SearchToolContext context) {
        var questions = new LinkedHashSet<String>();

        String keyword = shorten(context.keyword());
        if (!keyword.isEmpty()) {
            questions.add("「%s」里哪件性价比最高？".formatted(keyword));
        }
        context.topProducts().stream()
                .map(ProductReadModel::categoryName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .ifPresent(name -> questions.add("%s 类还有哪些选择？".formatted(name)));
        floorPrice(context.topProducts()).ifPresent(floor -> questions.add("预算 ¥%s 以内能买到什么？".formatted(floor)));

        return questions.stream().limit(MAX_QUESTIONS).toList();
    }

    private static String shorten(String keyword) {
        if (keyword == null) {
            return "";
        }
        String trimmed = keyword.strip();
        return trimmed.length() <= MAX_KEYWORD_CHARS ? trimmed : trimmed.substring(0, MAX_KEYWORD_CHARS);
    }

    /** 最低价向下取整到整数档（千元以上取百、以下取十），给出一个读起来像预算的整数。 */
    private static Optional<String> floorPrice(List<ProductReadModel> products) {
        return products.stream()
                .map(ProductReadModel::price)
                .filter(price -> price != null && price.signum() > 0)
                .min(BigDecimal::compareTo)
                .map(price -> price.compareTo(THOUSAND) >= 0
                        ? price.divide(HUNDRED, 0, RoundingMode.DOWN).multiply(HUNDRED)
                        : price.divide(BigDecimal.TEN, 0, RoundingMode.DOWN).multiply(BigDecimal.TEN))
                .map(value -> value.stripTrailingZeros().toPlainString());
    }
}

package com.cartethyia.easyorange.ai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 跨模块码表同步守卫 —— {@link AssetComparison} 按字面硬编了 product 模块的两个码表
 * （domain 层不能跨模块引用，ArchUnit Rule 1 白名单只放 JDK 与领域内部包），只写注释挡不住漂移：
 * 那边改了 {@code ConditionLevel.desc} 或加了状态码，这边的成色 / 状态维会**静默缺席**——不报错、
 * 不影响对话，只是模型少一个决策依据，日志里也看不出来。
 * <p>
 * 数据路径已核实：{@code AssetDetail.conditionDesc ← ConditionLevel.getDesc()}、
 * {@code AssetDetail.status ← ProductStatus.getCode()}（{@code ProductReadModelAssembler}），
 * 所以断言这两个枚举就是断言真实数据。
 * <p>
 * 断言行为而非私有常量：把枚举值当真值走一遍比对，读到预期结论才算同步；绕过私有可见性也避免
 * 「测试复制一份常量、两边一起错」。
 */
@DisplayName("AssetComparison 与 product 模块码表的同步守卫 -> 测试")
class AssetComparisonCodeTableSyncTest {

    private static final String DIMENSION_CONDITION = "成色";
    private static final String DIMENSION_STATUS = "在售状态";

    @Test
    @DisplayName("成色：按 ConditionLevel 声明顺序（值越小越新）逐级占优，四个 desc 全被识别")
    void conditionLevelOrderAndDescAreInSync() {
        ConditionLevel[] levels = ConditionLevel.values();
        assertThat(levels.length).isGreaterThanOrEqualTo(AssetComparison.MIN_CANDIDATES);

        for (int i = 0; i < levels.length - 1; i++) {
            ConditionLevel better = levels[i];
            ConditionLevel worse = levels[i + 1];

            var compared = AssetComparison.of(
                    List.of(candidate("better", better.getDesc()), candidate("worse", worse.getDesc())));

            assertThat(compared)
                    .as("档位 %s（desc=%s）应优于 %s（desc=%s）——不同步时成色维会静默缺席", better, better.getDesc(), worse, worse.getDesc())
                    .isPresent();
            assertThat(dimension(compared.orElseThrow(), DIMENSION_CONDITION).winnerProductId())
                    .as("胜出方应为 %s", better)
                    .isEqualTo("better");
        }
    }

    @Test
    @DisplayName("在售状态：只有 ProductStatus.ONLINE 算在售，其余 code 全被识别为非在售")
    void productStatusCodesAreInSync() {
        for (ProductStatus status : ProductStatus.values()) {
            if (status == ProductStatus.ONLINE) {
                continue;
            }

            var compared = AssetComparison.of(List.of(
                    candidateWithStatus("on-sale", ProductStatus.ONLINE.getCode()),
                    candidateWithStatus("off-sale", status.getCode())));

            assertThat(compared)
                    .as("状态码 %s（code=%s）应被识别为非在售——码表漂移时状态维会静默缺席", status, status.getCode())
                    .isPresent();
            AssetComparison.Dimension dimension = dimension(compared.orElseThrow(), DIMENSION_STATUS);
            assertThat(dimension.note()).contains(status.getCode()).contains("非在售");
            assertThat(dimension.winnerProductId()).as("唯一仍在售的那件胜出").isEqualTo("on-sale");
        }
    }

    /** 取某个维度的结论；缺席即断言失败，并把实际出现的维度列出来便于定位。 */
    private static AssetComparison.Dimension dimension(AssetComparison comparison, String name) {
        return comparison.dimensions().stream()
                .filter(dimension -> name.equals(dimension.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("维度「%s」缺席，实际只有 %s"
                        .formatted(
                                name,
                                comparison.dimensions().stream()
                                        .map(AssetComparison.Dimension::name)
                                        .toList())));
    }

    /** 成色维专用候选：价格 / 地区为空，状态固定 ONLINE，保证只有成色维进结果。 */
    private static AssetDetail candidate(String id, String conditionDesc) {
        return new AssetDetail(
                id, "资产 " + id, null, null, null, conditionDesc, null, null, ProductStatus.ONLINE.getCode());
    }

    /** 状态维专用候选：成色 / 地区为空，保证只有状态维进结果。 */
    private static AssetDetail candidateWithStatus(String id, String statusCode) {
        return new AssetDetail(id, "资产 " + id, null, null, null, null, null, null, statusCode);
    }
}

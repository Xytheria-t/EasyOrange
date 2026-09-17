package com.cartethyia.easyorange.ai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RrfFusion (排名融合) -> 测试")
class RrfFusionTest {

    @Test
    @DisplayName("两路都命中的文档排在只被单路命中的前面")
    void fuse_bothLegsRankHigher() {
        // 稠密路：b 第一；词面路：a 第一；c 只被一路命中
        var fused = RrfFusion.fuse(RrfFusion.DEFAULT_K, List.of(List.of("b", "c"), List.of("a", "c")));

        // c = 1/62 + 1/62 = 0.03226，b = a = 1/61 = 0.01639
        assertThat(fused.getFirst().id()).isEqualTo("c");
        assertThat(fused.getFirst().score()).isGreaterThan(fused.get(1).score());
        assertThat(fused).extracting(RrfFusion.Fused::id).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    @DisplayName("同一路内名次越靠前分越高（1/(k+rank)）")
    void fuse_rankWeightDecreases() {
        var fused = RrfFusion.fuse(RrfFusion.DEFAULT_K, List.of(List.of("first", "second", "third")));

        assertThat(fused.get(0).id()).isEqualTo("first");
        assertThat(fused.get(0).score()).isEqualTo(1.0 / 61);
        assertThat(fused.get(1).score()).isEqualTo(1.0 / 62);
        assertThat(fused.get(2).score()).isEqualTo(1.0 / 63);
    }

    @Test
    @DisplayName("同分按首次出现顺序稳定排序（评测可复现）")
    void fuse_tiesAreStable() {
        var first = RrfFusion.fuse(RrfFusion.DEFAULT_K, List.of(List.of("x"), List.of("y")));
        var second = RrfFusion.fuse(RrfFusion.DEFAULT_K, List.of(List.of("x"), List.of("y")));

        assertThat(first).extracting(RrfFusion.Fused::id).containsExactly("x", "y");
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("空输入与单路输入：空进空出，单路按原序返回")
    void fuse_degenerateInputs() {
        assertThat(RrfFusion.fuse(RrfFusion.DEFAULT_K, List.of())).isEmpty();
        assertThat(RrfFusion.fuse(RrfFusion.DEFAULT_K, List.of(List.of(), List.of())))
                .isEmpty();
        assertThat(RrfFusion.fuse(RrfFusion.DEFAULT_K, List.of(List.of("a", "b"), List.of())))
                .extracting(RrfFusion.Fused::id)
                .containsExactly("a", "b");
    }

    @Test
    @DisplayName("k 越小越强调头部名次")
    void fuse_smallerKAmplifiesTopRanks() {
        double smallKGap = scoreGap(1);
        double largeKGap = scoreGap(1000);

        assertThat(smallKGap).isGreaterThan(largeKGap);
    }

    private static double scoreGap(int k) {
        var fused = RrfFusion.fuse(k, List.of(List.of("a", "b")));
        return fused.get(0).score() - fused.get(1).score();
    }
}

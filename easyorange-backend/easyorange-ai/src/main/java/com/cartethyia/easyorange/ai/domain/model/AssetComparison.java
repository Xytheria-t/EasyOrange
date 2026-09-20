package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资产逐维比对 — compare_assets 工具的观察物：对 2~4 件候选做确定性比对，一次替代最多 4 次 product_detail。
 * <p>
 * 每维统一准入：<b>至少两件可判定，且判定值不全同</b>才进结果，否则该维静默缺席。缺席只表示「这一维给不出结论」，
 * 不表示候选相同——面议件不能当最低价胜出，成色自由文本读不出档位就不猜，状态码表外的取值按未知处理。
 * <p>
 * 码表是 product 模块的语言（{@code ConditionLevel} 的 desc / {@code ProductStatus} 的 code）；domain 层
 * 不能跨模块引用（ArchUnit Rule 1 白名单只放 JDK 与领域内部包），故按字面同步，改那边码表要同步改这里的常量。
 */
public final class AssetComparison {

    /** 少于 2 件无法比较。 */
    public static final int MIN_CANDIDATES = 2;

    /** 多于 4 件时观察文本会淹没下一轮 prompt，调用方负责截断。 */
    public static final int MAX_CANDIDATES = 4;

    private static final String DIMENSION_PRICE = "价格";
    private static final String DIMENSION_CONDITION = "成色";
    private static final String DIMENSION_LOCATION = "地区";
    private static final String DIMENSION_STATUS = "在售状态";

    /** 状态码全集（ProductStatus 的 code）；只有 {@link #STATUS_ON_SALE} 算在售。 */
    private static final Set<String> STATUS_CODES =
            Set.of("ONLINE", "OFFLINE", "SOLD", "DRAFT", "PENDING_REVIEW", "REJECTED");

    private static final String STATUS_ON_SALE = "ONLINE";

    private final List<Dimension> dimensions;
    private final String observation;

    private AssetComparison(List<Dimension> dimensions, String observation) {
        this.dimensions = dimensions;
        this.observation = observation;
    }

    /** 不足 MIN_CANDIDATES 件时返回 empty，由调用方转成模型可读的失败观察。 */
    public static Optional<AssetComparison> of(List<AssetDetail> details) {
        List<AssetDetail> candidates = details == null
                ? List.of()
                : details.stream().filter(Objects::nonNull).toList();
        if (candidates.size() < MIN_CANDIDATES) {
            return Optional.empty();
        }
        List<Dimension> dimensions = new ArrayList<>();
        priceDimension(candidates).ifPresent(dimensions::add);
        conditionDimension(candidates).ifPresent(dimensions::add);
        locationDimension(candidates).ifPresent(dimensions::add);
        statusDimension(candidates).ifPresent(dimensions::add);
        return Optional.of(new AssetComparison(List.copyOf(dimensions), observation(candidates, dimensions)));
    }

    /** 逐维差异（价格 / 成色 / 地区 / 在售状态），每维标出胜出方；无差异的维度不进结果。 */
    public List<Dimension> dimensions() {
        return dimensions;
    }

    /** 进下一轮 prompt 的观察文本：紧凑纯文本，不带 JSON 引号（同 AgentTools 的观察约定）。 */
    public String observation() {
        return observation;
    }

    /** 单维结论：维度名、胜出资产 ID（无胜出方时为 null）、一句话依据。 */
    public record Dimension(String name, String winnerProductId, String note) {}

    /**
     * 价格：胜出方 = 最低价。非正价是脏数据，按 MarketAnalysisTool 口径静默剔除；
     * 只有一件有有效价格（其余面议或价格缺失）时也不判定——单件不构成「更便宜」。
     */
    private static Optional<Dimension> priceDimension(List<AssetDetail> candidates) {
        List<AssetDetail> priced = candidates.stream()
                .filter(candidate ->
                        candidate.price() != null && candidate.price().signum() > 0)
                .toList();
        if (priced.size() < MIN_CANDIDATES) {
            return Optional.empty();
        }
        BigDecimal lowest = priced.stream()
                .map(AssetDetail::price)
                .min(BigDecimal::compareTo)
                .orElseThrow();
        List<AssetDetail> cheapest = priced.stream()
                .filter(candidate -> candidate.price().compareTo(lowest) == 0)
                .toList();
        if (cheapest.size() == priced.size()) {
            return Optional.empty();
        }
        String winner = cheapest.size() == 1 ? cheapest.getFirst().productId() : null;
        String note = winner == null
                ? "%s 同为最低 %s".formatted(ids(cheapest), money(lowest))
                : "%s 最低 %s".formatted(winner, money(lowest));
        return Optional.of(new Dimension(DIMENSION_PRICE, winner, note + negotiableSuffix(candidates)));
    }

    /** 成色：胜出方 = 档位最新的一件；读不出档位的候选退出该维比较，只在 note 里点明。 */
    private static Optional<Dimension> conditionDimension(List<AssetDetail> candidates) {
        List<AssetDetail> graded = new ArrayList<>();
        List<AssetDetail> unjudged = new ArrayList<>();
        for (AssetDetail candidate : candidates) {
            if (gradeOf(candidate.conditionDesc()) > 0) {
                graded.add(candidate);
            } else {
                unjudged.add(candidate);
            }
        }
        if (graded.size() < MIN_CANDIDATES) {
            return Optional.empty();
        }
        int best = graded.stream()
                .mapToInt(candidate -> gradeOf(candidate.conditionDesc()))
                .min()
                .orElseThrow();
        List<AssetDetail> bests = graded.stream()
                .filter(candidate -> gradeOf(candidate.conditionDesc()) == best)
                .toList();
        if (bests.size() == graded.size()) {
            return Optional.empty();
        }
        String winner = bests.size() == 1 ? bests.getFirst().productId() : null;
        String label = normalized(bests.getFirst().conditionDesc());
        String note =
                winner == null ? "%s 成色并列最好（%s）".formatted(ids(bests), label) : "%s 成色最好（%s）".formatted(winner, label);
        return Optional.of(
                new Dimension(DIMENSION_CONDITION, winner, note + unjudgedSuffix(unjudged, DIMENSION_CONDITION)));
    }

    /** 地区：没有客观优劣，胜出方恒为 null，只在候选分散在不同地区时列出事实。 */
    private static Optional<Dimension> locationDimension(List<AssetDetail> candidates) {
        List<AssetDetail> located = new ArrayList<>();
        List<AssetDetail> unjudged = new ArrayList<>();
        for (AssetDetail candidate : candidates) {
            if (normalized(candidate.location()) == null) {
                unjudged.add(candidate);
            } else {
                located.add(candidate);
            }
        }
        Set<String> distinct = located.stream()
                .map(candidate -> normalized(candidate.location()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinct.size() < MIN_CANDIDATES) {
            return Optional.empty();
        }
        String note = located.stream()
                        .map(candidate -> "%s %s".formatted(candidate.productId(), normalized(candidate.location())))
                        .collect(Collectors.joining("、"))
                + "，无客观优劣";
        return Optional.of(
                new Dimension(DIMENSION_LOCATION, null, note + unjudgedSuffix(unjudged, DIMENSION_LOCATION)));
    }

    /**
     * 在售状态：决定还推不推荐得出口，所以非在售必须显式点出。胜出方 = 唯一仍在售的那件
     * （只在其余候选都被判为非在售时才有意义）；码表外的取值按未知处理，不拿未知去下「非在售」的结论。
     */
    private static Optional<Dimension> statusDimension(List<AssetDetail> candidates) {
        List<AssetDetail> onSale = new ArrayList<>();
        List<AssetDetail> offSale = new ArrayList<>();
        List<AssetDetail> unjudged = new ArrayList<>();
        for (AssetDetail candidate : candidates) {
            String code = normalized(candidate.status());
            if (code == null || !STATUS_CODES.contains(code)) {
                unjudged.add(candidate);
            } else if (STATUS_ON_SALE.equals(code)) {
                onSale.add(candidate);
            } else {
                offSale.add(candidate);
            }
        }
        if (offSale.isEmpty()) {
            return Optional.empty();
        }
        String winner = onSale.size() == 1 ? onSale.getFirst().productId() : null;
        String note = offSale.stream()
                        .map(candidate -> "%s（%s）".formatted(candidate.productId(), normalized(candidate.status())))
                        .collect(Collectors.joining("、"))
                + "非在售，不建议推荐"
                + (winner == null ? "" : "，仅 %s 仍在售".formatted(winner))
                + unjudgedSuffix(unjudged, DIMENSION_STATUS);
        return Optional.of(new Dimension(DIMENSION_STATUS, winner, note));
    }

    /** 面议件不参与比价，但必须点出来——否则模型会把「没出现在最低价里」读成「不比最低价便宜」。 */
    private static String negotiableSuffix(List<AssetDetail> candidates) {
        List<AssetDetail> negotiable = candidates.stream()
                .filter(candidate -> candidate.price() == null)
                .toList();
        return negotiable.isEmpty() ? "" : "，%s 面议未参与比价".formatted(ids(negotiable));
    }

    /** 该维读不出值的候选要点明——否则模型会把「没被提到」当成「比过且不占优」。 */
    private static String unjudgedSuffix(List<AssetDetail> unjudged, String dimension) {
        return unjudged.isEmpty() ? "" : "，%s %s不可判定".formatted(ids(unjudged), dimension);
    }

    private static String ids(List<AssetDetail> candidates) {
        return candidates.stream().map(AssetDetail::productId).collect(Collectors.joining("、"));
    }

    /** 成色档位，值越小越新；0 = 自由文本读不出档位（此时该件退出成色比较，不硬编档位）。 */
    private static int gradeOf(String conditionDesc) {
        String text = normalized(conditionDesc);
        if (text == null) {
            return 0;
        }
        return switch (text) {
            case "全新" -> 1;
            case "几乎全新" -> 2;
            case "轻微使用痕迹" -> 3;
            case "明显使用痕迹" -> 4;
            default -> 0;
        };
    }

    /** 自由文本归一化：null / 空白一律视为「没有值」。返回值可能为 null，进码表查询前必须判空。 */
    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** 金额展示：去掉小数尾巴（¥4200 而不是 ¥4200.00）。 */
    private static String money(BigDecimal value) {
        return "¥" + value.stripTrailingZeros().toPlainString();
    }

    private static String observation(List<AssetDetail> candidates, List<Dimension> dimensions) {
        String header = "对比 %d 件：%s".formatted(candidates.size(), ids(candidates));
        if (dimensions.isEmpty()) {
            String allDimensions =
                    String.join(" / ", DIMENSION_PRICE, DIMENSION_CONDITION, DIMENSION_LOCATION, DIMENSION_STATUS);
            return header + "；%s 均无可判定差异，不构成选择依据".formatted(allDimensions);
        }
        return header
                + "；"
                + dimensions.stream()
                        .map(dimension -> dimension.name() + "：" + dimension.note())
                        .collect(Collectors.joining("；"));
    }
}

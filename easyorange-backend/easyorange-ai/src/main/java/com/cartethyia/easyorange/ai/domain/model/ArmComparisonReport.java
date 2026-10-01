package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;

/**
 * 双臂对照报告 — 同批用例在「有检索来源」与「无检索来源」两条臂上的 Judge 评分配对差分。
 * <p>
 * <b>按配对差分报数，不比两臂绝对分均值</b>：同一用例两臂的评分高度相关，独立样本口径会把用例难度的
 * 方差算进组间差里；配对差分把它消掉。绝对分只作组内定位（哪一臂更好），不作对外质量证据 ——
 * Judge 与被评模型同源时自评偏差对两臂同向，配对差可抵消，单臂绝对分不可对外引用。
 *
 * @param pairedCases  两臂都成功评分的用例数（分母口径，见 {@code missingArms}）
 * @param retrievalMean 有检索臂均分
 * @param noRetrievalMean 无检索臂均分
 * @param meanDiff      配对差分均值（正 = 有检索更优）
 * @param diffStdDev    配对差分标准差 —— n=22 量级下单轮方差的量级交代，判断差分是否只是噪声
 * @param missingArms   有一臂没评上分、用例被剔出配对的 caseId + 原因（前端不展示，排查用）
 */
public record ArmComparisonReport(
        int pairedCases,
        double retrievalMean,
        double noRetrievalMean,
        double meanDiff,
        double diffStdDev,
        List<String> missingArms) {}

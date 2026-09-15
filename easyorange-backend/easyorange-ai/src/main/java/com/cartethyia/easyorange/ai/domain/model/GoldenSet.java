package com.cartethyia.easyorange.ai.domain.model;

import java.util.List;

/** 金标准评测集 — 全部用例。 */
public record GoldenSet(List<GoldenSetCase> cases) {}

package com.cartethyia.easyorange.ai.dto;

public record QaResponse(
        String answer,
        boolean confidence
) {
}
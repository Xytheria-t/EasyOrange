package com.cartethyia.easyorange.ai.port;

import java.util.List;

public interface LlmPort {

    String generateText(String systemPrompt, String userMessage);

    List<Float> generateEmbedding(String text);

    String generateTextWithJson(String systemPrompt, String userMessage);
}
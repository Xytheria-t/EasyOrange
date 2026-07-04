package com.cartethyia.easyorange.ai.adapter;

import com.cartethyia.easyorange.ai.adapter.dto.DeepSeekRequest;
import com.cartethyia.easyorange.ai.adapter.dto.DeepSeekResponse;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.metrics.AiMetricsService;
import com.cartethyia.easyorange.ai.port.LlmPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class DeepSeekLlmAdapter implements LlmPort {

    private final RestClient deepseekRestClient;
    private final AiProperties aiProperties;
    private final AiMetricsService aiMetricsService;

    public DeepSeekLlmAdapter(RestClient deepseekRestClient, AiProperties aiProperties,
                              AiMetricsService aiMetricsService) {
        this.deepseekRestClient = deepseekRestClient;
        this.aiProperties = aiProperties;
        this.aiMetricsService = aiMetricsService;
    }

    @Override
    public String generateText(String systemPrompt, String userMessage) {
        var sample = aiMetricsService.startTimer();
        try {
            var request = new DeepSeekRequest(
                    aiProperties.getDeepseek().getModel(),
                    List.of(
                            new DeepSeekRequest.Message("system", systemPrompt),
                            new DeepSeekRequest.Message("user", userMessage)
                    )
            );

            var response = deepseekRestClient.post()
                    .uri("/v1/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(DeepSeekResponse.class);

            String content = response != null ? response.getFirstChoiceContent() : null;
            log.debug("DeepSeek generated text: length={}", content != null ? content.length() : 0);
            aiMetricsService.recordLlmDuration("REVIEW", sample, "success");
            return content;
        } catch (Exception e) {
            aiMetricsService.recordLlmDuration("REVIEW", sample, "error");
            throw e;
        }
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        var request = new java.util.HashMap<String, Object>();
        request.put("model", "deepseek-embedding");
        request.put("input", text);

        var response = deepseekRestClient.post()
                .uri("/v1/embeddings")
                .body(request)
                .retrieve()
                .body(DeepSeekEmbeddingResponse.class);

        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            return response.getData().get(0).getEmbedding();
        }
        log.warn("DeepSeek embedding returned empty result for text: {}", text);
        return List.of();
    }

    @Override
    public String generateTextWithJson(String systemPrompt, String userMessage) {
        var sample = aiMetricsService.startTimer();
        try {
            var request = new DeepSeekRequest(
                    aiProperties.getDeepseek().getModel(),
                    List.of(
                            new DeepSeekRequest.Message("system", systemPrompt),
                            new DeepSeekRequest.Message("user", userMessage)
                    ),
                    new DeepSeekRequest.ResponseFormat("json_object")
            );

            var response = deepseekRestClient.post()
                    .uri("/v1/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(DeepSeekResponse.class);

            String content = response != null ? response.getFirstChoiceContent() : null;
            aiMetricsService.recordLlmDuration("REVIEW", sample, "success");
            return content;
        } catch (Exception e) {
            aiMetricsService.recordLlmDuration("REVIEW", sample, "error");
            throw e;
        }
    }

    private static class DeepSeekEmbeddingResponse {
        private List<EmbeddingData> data;
        public List<EmbeddingData> getData() { return data; }
        public void setData(List<EmbeddingData> data) { this.data = data; }

        private static class EmbeddingData {
            private List<Float> embedding;
            public List<Float> getEmbedding() { return embedding; }
            public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
        }
    }
}
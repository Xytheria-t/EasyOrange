package com.cartethyia.easyorange.ai.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class QwenVlResponse {
    private Output output;
    private Usage usage;

    public Output getOutput() { return output; }
    public Usage getUsage() { return usage; }

    public String getFirstChoiceContent() {
        if (output != null && output.choices != null && !output.choices.isEmpty()) {
            return output.choices.get(0).message.content;
        }
        return null;
    }

    public static class Output {
        private List<Choice> choices;
        public List<Choice> getChoices() { return choices; }
    }

    public static class Choice {
        @JsonProperty("finish_reason")
        private String finishReason;
        private Message message;
        public Message getMessage() { return message; }
    }

    public static class Message {
        private String role;
        private String content;
        public String getContent() { return content; }
    }

    public static class Usage {
        @JsonProperty("total_tokens")
        private int totalTokens;
        public int getTotalTokens() { return totalTokens; }
    }
}
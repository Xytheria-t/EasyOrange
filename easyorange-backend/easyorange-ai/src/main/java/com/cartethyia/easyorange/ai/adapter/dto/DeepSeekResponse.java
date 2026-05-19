package com.cartethyia.easyorange.ai.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DeepSeekResponse {
    private List<Choice> choices;
    private Usage usage;

    public List<Choice> getChoices() { return choices; }
    public Usage getUsage() { return usage; }

    public String getFirstChoiceContent() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getMessage().getContent();
        }
        return null;
    }

    public static class Choice {
        private Message message;

        public Message getMessage() { return message; }
    }

    public static class Message {
        private String content;
        public String getContent() { return content; }
    }

    public static class Usage {
        @JsonProperty("total_tokens")
        private int totalTokens;
        public int getTotalTokens() { return totalTokens; }
    }
}
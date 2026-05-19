package com.cartethyia.easyorange.ai.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DeepSeekRequest {
    private String model;
    private List<Message> messages;
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    public DeepSeekRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public DeepSeekRequest(String model, List<Message> messages, ResponseFormat responseFormat) {
        this.model = model;
        this.messages = messages;
        this.responseFormat = responseFormat;
    }

    public String getModel() { return model; }
    public List<Message> getMessages() { return messages; }
    public ResponseFormat getResponseFormat() { return responseFormat; }

    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    public static class ResponseFormat {
        private String type;

        public ResponseFormat(String type) {
            this.type = type;
        }

        public String getType() { return type; }
    }
}
package com.cartethyia.easyorange.ai.adapter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class QwenVlRequest {
    private String model;
    private Input input;
    private Parameters parameters;

    public QwenVlRequest(String model, Input input) {
        this.model = model;
        this.input = input;
    }

    public String getModel() { return model; }
    public Input getInput() { return input; }
    public Parameters getParameters() { return parameters; }
    public void setParameters(Parameters parameters) { this.parameters = parameters; }

    public static class Input {
        private List<Message> messages;

        public Input(List<Message> messages) {
            this.messages = messages;
        }

        public List<Message> getMessages() { return messages; }
    }

    public static class Message {
        private String role;
        private List<Content> content;

        public Message(String role, List<Content> content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public List<Content> getContent() { return content; }
    }

    public static class Content {
        private String type;
        private String text;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String imageUrl;

        public Content(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public Content(String type, String text, String imageUrl) {
            this.type = type;
            this.text = text;
            this.imageUrl = imageUrl;
        }

        public String getType() { return type; }
        public String getText() { return text; }
        public String getImageUrl() { return imageUrl; }
    }

    public static class Parameters {
        @JsonProperty("result_format")
        private String resultFormat = "message";

        public String getResultFormat() { return resultFormat; }
    }
}
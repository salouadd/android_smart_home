package com.example.smarthome;

import java.util.List;

class GroqRequest {
    String model;
    List<Message> messages;

    public GroqRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static class Message {
        String role;
        String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}

class GroqResponse {
    List<Choice> choices;

    public static class Choice {
        Message message;
    }

    public static class Message {
        String content;
    }
}

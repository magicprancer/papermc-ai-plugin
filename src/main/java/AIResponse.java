package com.example.aiplugin;

import com.google.gson.JsonObject;

public class AIResponse {
    private final String content;
    private final JsonObject functionCall;

    public AIResponse(String content, JsonObject functionCall) {
        this.content = content;
        this.functionCall = functionCall;
    }

    public String getContent() { return content; }
    public JsonObject getFunctionCall() { return functionCall; }
}

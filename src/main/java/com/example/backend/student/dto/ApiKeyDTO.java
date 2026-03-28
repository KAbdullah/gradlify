/**
 * DTO for transferring the AI API key between frontend and backend.
 */

package com.example.backend.student.dto;

public class ApiKeyDTO {
    private String apiKey;
    private String modelName;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}


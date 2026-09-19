package com.prizm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey = "";
    private String tagModel = "gemini-2.5-flash";
    private String embedModel = "gemini-embedding-001";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getTagModel() {
        return tagModel;
    }

    public void setTagModel(String tagModel) {
        this.tagModel = tagModel;
    }

    public String getEmbedModel() {
        return embedModel;
    }

    public void setEmbedModel(String embedModel) {
        this.embedModel = embedModel;
    }
}

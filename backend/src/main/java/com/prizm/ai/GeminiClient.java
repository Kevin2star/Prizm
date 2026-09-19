package com.prizm.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prizm.config.GeminiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final int EMBED_DIM = 768;
    private static final int MAX_EMBED_CHARS = 8000;

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    public List<String> generateTags(String title, String content, String school, String major) {
        String prompt = """
                다음 결과물에서 핵심 주제, 접근 방식, 사용 기술/관점을 한국어 명사구 태그 3~5개로 추출하라.
                전공명을 태그로 그대로 넣지 마라. JSON만 반환하고 마크다운 코드펜스를 쓰지 마라.
                스키마: {"tags":["키워드1","키워드2","키워드3"]}
                제목: %s
                학교/전공(맥락일 뿐 태그 금지): %s / %s
                본문:
                %s
                """.formatted(title, school, major, content);
        String text = generateContent(properties.getTagModel(), prompt);
        return parseTags(text);
    }

    public double[] embed(String title, String content) {
        String input = title + "\n" + content;
        if (input.length() > MAX_EMBED_CHARS) {
            input = input.substring(0, MAX_EMBED_CHARS);
        }
        Map<String, Object> body = Map.of(
                "model", "models/" + properties.getEmbedModel(),
                "content", Map.of("parts", List.of(Map.of("text", input))),
                "outputDimensionality", EMBED_DIM
        );
        try {
            String response = restClient.post()
                    .uri("/models/{model}:embedContent?key={key}", properties.getEmbedModel(), properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode values = objectMapper.readTree(response).path("embedding").path("values");
            if (!values.isArray() || values.isEmpty()) {
                throw new IllegalStateException("empty embedding");
            }
            double[] vector = new double[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i).asDouble();
            }
            return vector;
        } catch (RestClientResponseException ex) {
            log.warn("Gemini embed failed: status={}", ex.getStatusCode().value());
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to embed content", ex);
        }
    }

    public String generateContent(String model, String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.2)
        );
        try {
            String response = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode candidates = objectMapper.readTree(response).path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalStateException("empty generateContent response");
            }
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
        } catch (RestClientResponseException ex) {
            log.warn("Gemini generateContent failed: status={}", ex.getStatusCode().value());
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate content", ex);
        }
    }

    public String stripJson(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return text.substring(start, end + 1);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private List<String> parseTags(String raw) {
        try {
            JsonNode node = objectMapper.readTree(stripJson(raw)).path("tags");
            List<String> tags = new ArrayList<>();
            if (node.isArray()) {
                node.forEach(item -> {
                    if (!item.asText().isBlank()) {
                        tags.add(item.asText());
                    }
                });
            }
            return tags;
        } catch (Exception ex) {
            return List.of();
        }
    }
}

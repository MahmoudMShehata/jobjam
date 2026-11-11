package com.jobjam.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Extracts with a vision model served by a local Ollama, free of charge. The model cannot
 * browse, so posting URLs are downloaded by {@link PageFetcher} and passed in as text.
 */
@Service
@ConditionalOnProperty(name = "jobjam.ai.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaJobExtractor implements JobExtractor {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    // Ollama constrains the reply to this JSON schema, the same shape Claude returns. The model
    // writes the properties in this order, so "problem" must come last: asked for it first,
    // models fail to read the posting and loop.
    static final JsonNode EXTRACTION_SCHEMA = JSON.readTree("""
            {
              "type": "object",
              "properties": {
                "title": {"type": "string"},
                "description": {"type": "string"},
                "requirements": {"type": "string"},
                "benefits": {"type": "string"},
                "details": {"type": "string"},
                "problem": {"type": "string"}
              },
              "required": ["title", "description", "requirements", "benefits", "details", "problem"]
            }""");

    // Sampling Qwen recommends for its VL instruct models; greedy decoding (temperature 0) lets
    // small models fall into repeating themselves, which the presence penalty discourages.
    private static final Map<String, Object> SAMPLING = Map.of(
            "temperature", 0.7, "top_p", 0.8, "top_k", 20, "presence_penalty", 1.5);

    // A long posting takes about 1,500 tokens; past this the model is looping, so fail fast.
    private static final int MAX_REPLY_TOKENS = 4000;

    private final String baseUrl;
    private final String model;
    private final int contextLength;
    private final PageFetcher pages;
    private final RestClient ollama;

    public OllamaJobExtractor(@Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model,
            @Value("${ollama.context-length}") int contextLength,
            @Value("${ollama.timeout}") Duration timeout,
            PageFetcher pages) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.contextLength = contextLength;
        this.pages = pages;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(timeout);
        this.ollama = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    record ChatResponse(Message message, @JsonProperty("done_reason") String doneReason) {
    }

    record Message(String content) {
    }

    @Override
    public JobDraft extract(String url, String postingText, List<Screenshot> screenshots) {
        String source = url == null || url.isBlank() ? "" : "Source URL: " + url + "\n\n";
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        if (!screenshots.isEmpty()) {
            userMessage.put("content", source + "Extract the job posting shown in these screenshots.");
            userMessage.put("images", screenshots.stream()
                    .map(screenshot -> Base64.getEncoder().encodeToString(screenshot.data()))
                    .toList());
        } else if (postingText != null && !postingText.isBlank()) {
            userMessage.put("content", source + posting(postingText.strip()));
        } else if (url != null && !url.isBlank()) {
            userMessage.put("content", source + posting(pages.fetch(url)));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide a job URL, the posting text or screenshots");
        }

        Map<String, Object> request = Map.of(
                "model", model,
                "stream", false,
                // Thinking models otherwise reason for thousands of tokens before answering.
                "think", false,
                "format", EXTRACTION_SCHEMA,
                "options", options(),
                "messages", List.of(Map.of("role", "system", "content", SYSTEM_PROMPT), userMessage));

        ChatResponse response = chat(request);
        if ("length".equals(response.doneReason())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The local model's answer was cut off; try again, or paste only the job description");
        }
        try {
            return JSON.readValue(response.message().content(), Extraction.class).toDraft();
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The local model returned an unreadable extraction, please try again");
        }
    }

    private Map<String, Object> options() {
        Map<String, Object> options = new LinkedHashMap<>(SAMPLING);
        options.put("num_ctx", contextLength);
        options.put("num_predict", MAX_REPLY_TOKENS);
        return options;
    }

    private static String posting(String text) {
        if (text.length() > PageFetcher.MAX_TEXT_CHARS) {
            text = text.substring(0, PageFetcher.MAX_TEXT_CHARS);
        }
        return "<posting>\n" + text + "\n</posting>";
    }

    private ChatResponse chat(Map<String, Object> request) {
        try {
            return ollama.post().uri("/api/chat").body(request).retrieve().body(ChatResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI model " + model + " is not installed: run `ollama pull " + model + "`");
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The local model failed: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof HttpTimeoutException) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                        "The local model took too long, please try again");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI extraction is not available: Ollama is not running at " + baseUrl
                            + " (start it with `brew services start ollama`)");
        }
    }
}

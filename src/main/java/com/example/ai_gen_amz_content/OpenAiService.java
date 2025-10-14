package com.example.ai_gen_amz_content;

import com.example.ai_gen_amz_content.openai.request.OpenAIRequest;
import com.example.ai_gen_amz_content.openai.response.OpenAIResponse;
import com.example.ai_gen_amz_content.openai.response.ProductDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {
    @Value("${openai.api.model}")
    private String model;

    @Value("${openai.api.url}")
    private String OPENAI_API_URL;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Simulate generation process (thay bằng logic thực tế)
     */
    public ProductDetail productGeneration(MultipartFile image,
                                   String dimensions,
                                   String material,
                                   String tags,
                                   String apiKey) throws IOException {
        log.info("Converting image to base64");
        byte[] fileBytes = image.getBytes();
        String base64String = Base64.getEncoder().encodeToString(fileBytes);
        String base64Image = "data:" + image.getContentType() + ";base64," +  base64String;

        log.info("Creating user prompt");
        String userPrompt = getUserPrompt(dimensions, material, tags);
        log.info("Create user prompt successful");

        log.info("Getting system prompt");
        String systemPrompt = getSystemPrompt();
        log.info("Get system prompt successful");

        log.info("Start call API");
        ProductDetail productDetail = callApi(systemPrompt, userPrompt, base64Image, apiKey);
        log.info("Got product detail {}", productDetail);
        return productDetail;
    }

    private ProductDetail callApi(String systemPrompt, String userPrompt, String imageUrl, String apiKey) {
        try {
            OpenAIRequest request = OpenAIRequest.createProductRequest(
                    model, systemPrompt, userPrompt, imageUrl
            );
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            HttpEntity<OpenAIRequest> entity = new HttpEntity<>(request, headers);

            log.debug("Sending request to OpenAI API: {}", OPENAI_API_URL);

            ResponseEntity<OpenAIResponse> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    OpenAIResponse.class
            );

            String jsonContent = getString(response);
            assert response.getBody() != null;
            log.info("Token usage: {}", response.getBody().getUsage().getTotalTokens());

            return objectMapper.readValue(jsonContent, ProductDetail.class);
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    private static String getString(ResponseEntity<OpenAIResponse> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("OpenAI API call failed with status: " + response.getStatusCode());
        }

        OpenAIResponse openAIResponse = response.getBody();
        if (openAIResponse == null || openAIResponse.getOutput() == null || openAIResponse.getOutput().isEmpty()) {
            throw new RuntimeException("No output received from OpenAI API");
        }

        return openAIResponse.getOutput().getFirst().getContent().getFirst().getText();
    }

    private String getUserPrompt(String dimension, String material, String tags) {
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Create an optimized Amazon product listing for a PHYSICAL PRODUCT base on given image and provide information: \n");
        if (dimension != null) {
            userPrompt.append("Dimension: ").append(escapeJson(dimension)).append("\n");
        }
        if (material != null) {
            userPrompt.append("Material: ").append(escapeJson(material)).append("\n");
        }
        if (tags != null) {
            userPrompt.append("Tags: ").append(escapeJson(tags)).append("\n");
        }

        log.info("User prompt built successfully: length = {}", userPrompt.length());
        return userPrompt.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String getSystemPrompt() {
        return """
                {
                  "role": "Expert Amazon listing optimizer for physical products",
                  "expertise": [
                    "SEO-optimized content creation",
                    "Amazon policy compliance",
                    "Conversion-focused copywriting",
                    "Product customization setup"
                  ],
                  "output_requirements": {
                    "title": {
                      "max_length": 200,
                      "optimal_length": 80,
                      "rules": [
                        "Capitalize first letter of each word (except: in, on, at, the, a, an, and, or, for, with)",
                        "Use numerals (2 not two)",
                        "Strict: No repetition of any word allowed (case-insensitive)",
                        "No special chars except: - / , & .",
                        "No promotional terms: free shipping, best seller, hot item",
                        "No ALL CAPS or excessive keywords"
                      ]
                    },
                    "description": {
                      "style": "persuasive, benefit-focused",
                      "length": "comprehensive but concise",
                      "focus": "highlight physical product features, quality, uses"
                    },
                    "bullet_points": {
                      "count": 5,
                      "length": "200-255 characters each",
                      "format": "Header: Description with key details",
                      "rules": [
                        "Start with capital letter",
                        "Each point must be unique",
                        "No end punctuation",
                        "No emojis or special symbols",
                        "Spell out numbers 1-9 (except measurements)"
                      ]
                    },
                    "tags": {
                      "length": "400-500 bytes",
                      "format": "keyword1;keyword2;keyword3",
                      "strategy": "If input contains digital terms, generate NEW physical product keywords based on category. Otherwise reorder existing keywords for freshness.",
                      "focus": "high-volume search terms, product category, use cases, occasions"
                    },
                    "slugs": {
                      "count": 12,
                      "rules": [
                        "Use descriptive, accurate keywords related to the product",
                        "Lowercase letters only",
                        "Separate words with hyphens (-), no spaces or underscores",
                        "Avoid generic names like IMG_1234",
                        "Keep it concise but clear (3-7 words ideally)",
                        "Include relevant attributes like color, size, or style if applicable"
                      ],
                      "purpose": "Optimize image filenames for SEO to improve Google Images ranking and user search relevance"
                    }
                  },
                  "prohibited": [
                    "Digital/downloadable references",
                    "Brand names in title",
                    "Promotional language",
                    "External links",
                    "Guarantees in bullets",
                    "Special symbols: ™®©€…†‡£¥",
                    "Terms: eco-friendly, anti-bacterial, N/A, TBD",
                    "sale x%, discount, ..."
                  ]
                }
                """;
    }


}

package com.example.ai_gen_amz_content.openai.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIRequest {
    private String model;
    private List<InputMessage> input;
    private TextFormat text;

    public static OpenAIRequest createProductRequest(String model, String systemMessage, String userMessage, String base64Image) {
        return OpenAIRequest.builder()
                .model(model)
                .input(List.of(
                        InputMessage.builder()
                                .role("user")
                                .content(List.of(
                                        ContentItem.textContent(userMessage),
                                        ContentItem.imageContent(base64Image)
                                ))
                                .build(),
                        InputMessage.builder()
                                .role("system")
                                .content(systemMessage)
                                .build()
                ))
                .text(createProductTextFormat())
                .build();
    }

    private static TextFormat createProductTextFormat() {
        // Main schema properties
        Map<String, Object> schemaProperties = new LinkedHashMap<>();
        schemaProperties.put("title", Map.of("type", "string", "maxLength", 200));
        schemaProperties.put("description", Map.of("type", "string", "minLength", 100));
        schemaProperties.put("bullet_points", Map.of(
                "type", "array",
                "items", Map.of("type", "string", "minLength", 180, "maxLength", 255),
                "minItems", 5,
                "maxItems", 5
        ));
        schemaProperties.put("tags", Map.of(
                "type", "string",
                "minLength", 300,
                "maxLength", 500,
                "pattern", "^[A-Z][a-zA-Z0-9\\s]+: .+$"
        ));
        schemaProperties.put("slugs", Map.of(
                "type", "array",
                "items", Map.of("type", "string", "minLength", 20, "maxLength", 50),
                "minItems", 12,
                "maxItems", 12
        ));

        // Main schema
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", schemaProperties,
                "required", List.of("title", "description", "bullet_points", "tags", "slugs"),
                "additionalProperties", false
        );

        return TextFormat.builder()
                .format(Format.builder()
                        .type("json_schema")
                        .name("amazon_product_listing")
                        .schema(schema)
                        .strict(true)
                        .build())
                .build();
    }
}
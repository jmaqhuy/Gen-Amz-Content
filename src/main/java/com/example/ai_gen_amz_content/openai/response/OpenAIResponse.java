package com.example.ai_gen_amz_content.openai.response;

import com.example.ai_gen_amz_content.openai.request.TextFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIResponse {
    private String id;
    private String object;

    @JsonProperty("created_at")
    private long createdAt;

    private String status;
    private boolean background;
    private Object error;

    @JsonProperty("incomplete_details")
    private Object incompleteDetails;

    private Object instructions;

    @JsonProperty("max_output_tokens")
    private Object maxOutputTokens;

    @JsonProperty("max_tool_calls")
    private Object maxToolCalls;

    private String model;
    private List<OutputMessage> output;

    @JsonProperty("parallel_tool_calls")
    private boolean parallelToolCalls;

    @JsonProperty("previous_response_id")
    private Object previousResponseId;

    @JsonProperty("prompt_cache_key")
    private Object promptCacheKey;

    private Reasoning reasoning;

    @JsonProperty("safety_identifier")
    private Object safetyIdentifier;

    @JsonProperty("service_tier")
    private String serviceTier;

    private boolean store;
    private double temperature;
    private TextFormat text;

    @JsonProperty("tool_choice")
    private String toolChoice;

    private List<Object> tools;

    @JsonProperty("top_logprobs")
    private int topLogprobs;

    @JsonProperty("top_p")
    private double topP;

    private String truncation;
    private Usage usage;
    private Object user;
    private Object metadata;
}

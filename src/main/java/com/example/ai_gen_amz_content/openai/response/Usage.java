package com.example.ai_gen_amz_content.openai.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usage {
    @JsonProperty("input_tokens")
    private int inputTokens;

    @JsonProperty("input_tokens_details")
    private InputTokensDetails inputTokensDetails;

    @JsonProperty("output_tokens")
    private int outputTokens;

    @JsonProperty("output_tokens_details")
    private OutputTokensDetails outputTokensDetails;

    @JsonProperty("total_tokens")
    private int totalTokens;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputTokensDetails {
        @JsonProperty("cached_tokens")
        private int cachedTokens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputTokensDetails {
        @JsonProperty("reasoning_tokens")
        private int reasoningTokens;
    }
}
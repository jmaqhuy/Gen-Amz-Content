package com.example.ai_gen_amz_content.openai.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputMessage {
    private String role;
    private Object content; // Can be String or List<ContentItem>
}
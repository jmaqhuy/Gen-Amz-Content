package com.example.ai_gen_amz_content.openai.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Format {
    private String type;
    private String name;
    private Map<String, Object> schema;
    private boolean strict;
}

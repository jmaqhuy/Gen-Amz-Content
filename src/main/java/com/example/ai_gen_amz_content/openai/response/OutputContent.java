package com.example.ai_gen_amz_content.openai.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutputContent {
    private String type;
    private List<Object> annotations;
    private List<Object> logprobs;
    private String text;
}

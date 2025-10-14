package com.example.ai_gen_amz_content.openai.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutputMessage {
    private String id;
    private String type;
    private String status;
    private List<OutputContent> content;
    private String role;
}
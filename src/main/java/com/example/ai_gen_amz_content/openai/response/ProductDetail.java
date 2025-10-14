package com.example.ai_gen_amz_content.openai.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProductDetail {
    private String title;
    private String description;
    @JsonProperty("bullet_points")
    private List<String> bulletPoints;
    private String tags;
    private List<String> slugs;
}

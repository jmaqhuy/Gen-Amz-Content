package com.example.ai_gen_amz_content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GenController {

    private final OpenAiService openAiService;

    @PostMapping("/api/generate-content")
    @ResponseBody
    public ResponseEntity<?> generateContent(@RequestParam("productImage") MultipartFile image,
                                             @RequestParam(required = false) String dimensions,
                                             @RequestParam(required = false) String material,
                                             @RequestParam(required = false) String tags,
                                             @RequestParam("apiKey") String apiKey
    ) {
        try {
            return ResponseEntity.ok().body(openAiService.productGeneration(image, dimensions, material, tags, apiKey));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

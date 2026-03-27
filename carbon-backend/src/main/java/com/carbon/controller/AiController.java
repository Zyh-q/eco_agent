package com.carbon.controller;

import com.carbon.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> params) {
        String question = params.get("question").toString();
        Long userId = Long.valueOf(params.get("userId").toString());

        String answer = aiService.chatWithAi(question, userId);

        return Map.of(
                "code", 200,
                "data", Map.of(
                        "content", answer,
                        "suggestion", ""
                )
        );
    }
}
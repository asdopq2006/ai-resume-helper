package com.example.airesumehelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    public AiController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @PostMapping("/analyze-resume")
    public Map<String, Object> analyzeResume(@RequestBody String resumeText) {
        String systemPrompt = "你是专业HR。从简历中提取以下信息，严格按JSON格式输出：\n" +
                "{\"skills\":[\"skill1\",\"skill2\"], \"yearsOfExperience\":数字, \"education\":\"本科/硕士等\", \"expectedPosition\":\"职位名\"}";
        String aiResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(resumeText)
                .call()
                .content();

        // 解析 JSON 字符串为 Map
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(aiResponse, Map.class);
        } catch (Exception e) {
            return Map.of("error", "AI 返回格式异常：" + e.getMessage());
        }
    }
    @PostMapping("/analyze-resume-file")
    public Map<String, Object> analyzeResumeFile(@RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return analyzeResume(content); // 复用之前的 analyzeResume 方法
    }
}

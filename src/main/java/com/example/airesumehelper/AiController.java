package com.example.airesumehelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private AnalysisHistoryRepository historyRepository;


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
        try {
            Map<String, Object> result = parseResume(resumeText);
            saveHistory(resumeText, result);
            return result;
        } catch (Exception e) {
            return Map.of("error", "AI返回格式异常：" + e.getMessage());
        }
    }
    @PostMapping("/analyze-resume-file")
    public Map<String, Object> analyzeResumeFile(@RequestParam("file") MultipartFile file)  {
        try {
            // 1. 提取文件中的文本
            String resumeText = FileTextExtractor.extractText(file);

            // 2. 调用AI分析（复用已有逻辑）
            Map<String, Object> result = parseResume(resumeText);

            // 3. 保存历史记录（如果AI没有返回错误）
            if (!result.containsKey("error")) {
                saveHistory(resumeText, result);
            }
            return result;
        } catch (Exception e) {
            return Map.of("error", "文件处理失败：" + e.getMessage());
        }
    }

    private void saveHistory(String resumeText, Map<String, Object> result) {
        List<String> skillsList = (List<String>) result.get("skills");
        String skills = skillsList != null ? String.join(",", skillsList) : "";
        Integer years = (Integer) result.get("yearsOfExperience");
        String education = (String) result.get("education");
        String expectedPosition = (String) result.get("expectedPosition");

        AnalysisHistory history = new AnalysisHistory();
        history.setResumeText(resumeText);
        history.setSkills(skills);
        history.setYearsOfExperience(years);
        history.setEducation(education != null ? education : "");
        history.setExpectedPosition(expectedPosition != null ? expectedPosition : "");
        history.setCreateTime(LocalDateTime.now());
        historyRepository.save(history);
    }

    private Map<String, Object> parseResume(String resumeText) throws Exception {
        String systemPrompt = "你是专业HR。从简历中提取以下信息，严格按JSON格式输出，所有字段都必须包含，缺失字段用空字符串或null表示：\n" +
                "{\"skills\":[\"skill1\",\"skill2\"], \"yearsOfExperience\":数字, \"education\":\"本科/硕士等\", \"expectedPosition\":\"职位名\"}";
        String aiResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(resumeText)
                .call()
                .content();
        return objectMapper.readValue(aiResponse, Map.class);
    }

    @GetMapping("/history")
    public List<AnalysisHistory> getHistory() {
        return historyRepository.findAllByOrderByCreateTimeDesc();
    }

    @DeleteMapping("/history/{id}")
    public void deleteHistory(@PathVariable Long id) {
        historyRepository.deleteById(id);
    }
}

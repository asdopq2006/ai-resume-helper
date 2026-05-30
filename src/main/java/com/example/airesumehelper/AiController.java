package com.example.airesumehelper;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @PostMapping("/analyze-resume-batch")
    public void analyzeResumeBatch(@RequestParam("files") List<MultipartFile> files, HttpServletResponse response) {
        // 创建 Excel 工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("简历分析结果");

        // 创建标题行
        Row header = sheet.createRow(0);
        String[] columns = {"文件名", "技能", "工作年限", "学历", "期望职位", "简历原文"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        int rowNum = 1;
        for (MultipartFile file : files) {
            try {
                // 提取文件文本
                String resumeText = FileTextExtractor.extractText(file);
                // 调用 AI 分析（复用 parseResume 方法）
                Map<String, Object> result = parseResume(resumeText);

                // 提取字段
                List<String> skillsList = (List<String>) result.get("skills");
                String skills = skillsList != null ? String.join(",", skillsList) : "";
                Integer years = (Integer) result.get("yearsOfExperience");
                String education = (String) result.get("education");
                String expectedPosition = (String) result.get("expectedPosition");

                // 写入一行
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(file.getOriginalFilename());
                row.createCell(1).setCellValue(skills);
                row.createCell(2).setCellValue(years != null ? String.valueOf(years) : "");
                row.createCell(3).setCellValue(education != null ? education : "");
                row.createCell(4).setCellValue(expectedPosition != null ? expectedPosition : "");
                row.createCell(5).setCellValue(resumeText.length() > 200 ? resumeText.substring(0, 200) + "..." : resumeText);

                // 可选：保存每条记录到数据库（如果需要）
                // saveHistory(resumeText, result);
            } catch (Exception e) {
                // 写入错误行
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(file.getOriginalFilename());
                row.createCell(1).setCellValue("解析失败: " + e.getMessage());
            }
        }

        // 自动调整列宽
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 输出到响应
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=resume_analysis.xlsx");
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException("生成Excel失败", e);
        }
    }

    // 辅助方法：创建表头样式（可选）
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
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

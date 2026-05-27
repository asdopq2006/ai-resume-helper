package com.example.airesumehelper;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileTextExtractor {
    public static String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 安全地提取扩展名
        String ext = getFileExtension(fileName);
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("无法识别的文件类型，请提供 .txt .pdf .docx 文件");
        }

        try (InputStream is = file.getInputStream()) {
            switch (ext.toLowerCase()) {
                case "txt":
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                case "pdf":
                    return extractPdf(is);
                case "docx":
                    return extractDocx(is);
                default:
                    throw new IllegalArgumentException("不支持的文件类型: " + ext);
            }
        }
    }

    private static String extractPdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static String extractDocx(InputStream is) throws IOException {
        try (XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 安全地获取文件扩展名（小写，不带点）
     * 处理：无扩展名、以点开头、文件名仅为点等情况
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        // 没有点，或者点是第一个字符（隐藏文件如 .gitignore）且后面无字符，则无有效扩展名
        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}

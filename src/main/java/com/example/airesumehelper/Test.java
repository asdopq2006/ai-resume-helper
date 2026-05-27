package com.example.airesumehelper;

public class Test {
    public static void main(String[] args) {

        String fileName = "ta";
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        int lastDot = fileName.lastIndexOf('.');
        System.out.printf("lastDot="+lastDot);
        // 安全地提取扩展名
        String ext = getFileExtension(fileName);
        System.out.println("lastDot="+lastDot);
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("无法识别的文件类型，请提供 .txt .pdf .docx 文件");
        }


    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        // 没有点，或者点是第一个字符（隐藏文件如 .gitignore）且后面无字符，则无有效扩展名
        if (lastDot <= 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}

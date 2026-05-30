# AI 简历分析助手

基于 Spring Boot + 硅基流动 API 的智能简历分析工具，支持文本/文件上传，自动提取技能、工作年限、学历、期望职位。

## 技术栈
- Spring Boot 3.2
- Spring AI (OpenAI 兼容)
- H2 Database + JPA
- Bootstrap 5
- Maven

## 功能
- 文本粘贴分析
- 单文件上传（.txt/.pdf/.docx）
- 批量文件上传，导出 Excel 报告
- 历史记录保存与删除
- 响应式界面 + 加载动画

## 部署
- Railway 在线地址：[https://ai-resume-helper.up.railway.app](https://ai-resume-helper.up.railway.app)


## 本地运行
1. 克隆仓库
2. 配置 `application.yml` 中的 `SILICONFLOW_API_KEY`
3. 运行 `AiResumeHelperApplication`
4. 访问 `http://localhost:8080`
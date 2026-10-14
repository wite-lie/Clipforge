package com.clipforge.rag;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * StaticCreativeKnowledgeRetriever（静态创作知识检索器）。
 *
 * <p>该组件从 data/knowledge（静态知识库目录）读取 Markdown（标记文档）文件，并按平台返回创作知识。
 *
 * <p>当前实现是 Static RAG Prototype（静态检索增强原型），还不是向量 RAG（检索增强生成）。
 */
@Component
public class StaticCreativeKnowledgeRetriever {

    /** knowledgeBasePath（知识库基础路径）表示 Markdown（标记文档）知识库所在目录。 */
    @Value("${clipforge.knowledge.base-path:data/knowledge}")
    private String knowledgeBasePath = "data/knowledge";

    /**
     * retrieveByPlatform（按平台检索）用于读取平台知识、脚本结构和评估提示。
     *
     * @param platform 目标平台
     * @return 创作知识
     */
    public CreativeKnowledge retrieveByPlatform(String platform) {
        // 把用户传入的平台名转换为知识库文件名。
        String platformFileName = resolvePlatformFileName(platform);
        // 读取平台 Markdown（标记文档）正文。
        String platformMarkdown = readKnowledgeFile(Path.of(knowledgeBasePath, "platforms", platformFileName));
        // 从平台文档中解析平台规则。
        List<String> platformRules = parseBulletsUnderHeading(platformMarkdown, "平台规则");
        // 从平台文档中解析脚本结构。
        List<String> scriptPatterns = parseBulletsUnderHeading(platformMarkdown, "脚本结构");
        // 从平台文档中解析评估提示。
        List<String> evaluationHints = parseBulletsUnderHeading(platformMarkdown, "评估提示");
        // 如果平台文档没有脚本结构，则从通用脚本模板中补充。
        if (scriptPatterns.isEmpty()) {
            // 读取通用脚本结构模板。
            String commonScriptMarkdown = readKnowledgeFile(Path.of(knowledgeBasePath, "script-patterns.md"));
            // 解析通用脚本结构。
            scriptPatterns = parseBulletsUnderHeading(commonScriptMarkdown, "通用脚本结构");
        }
        // 如果平台文档没有评估提示，则从通用评估标准中补充。
        if (evaluationHints.isEmpty()) {
            // 读取通用评估标准。
            String rubricMarkdown = readKnowledgeFile(Path.of(knowledgeBasePath, "evaluation-rubric.md"));
            // 解析评估维度作为兜底评估提示。
            evaluationHints = parseBulletsUnderHeading(rubricMarkdown, "评估维度");
        }
        // 创建并返回创作知识对象。
        return new CreativeKnowledge(platform, platformRules, scriptPatterns, evaluationHints);
    }

    /**
     * resolvePlatformFileName（解析平台文件名）用于把平台别名映射到 Markdown（标记文档）文件。
     *
     * @param platform 目标平台
     * @return 平台知识文件名
     */
    private String resolvePlatformFileName(String platform) {
        // 把平台名转成小写，方便匹配英文别名。
        String normalizedPlatform = platform.toLowerCase(Locale.ROOT);
        // 根据平台别名返回对应知识文件名。
        return switch (normalizedPlatform) {
            // 抖音平台知识文件。
            case "douyin", "抖音" -> "douyin.md";
            // 小红书平台知识文件。
            case "xiaohongshu", "小红书" -> "xiaohongshu.md";
            // Bilibili（哔哩哔哩）平台知识文件。
            case "bilibili", "b站", "哔哩哔哩" -> "bilibili.md";
            // TikTok（海外短视频平台）平台知识文件。
            case "tiktok" -> "tiktok.md";
            // 未支持平台使用默认知识文件。
            default -> "default.md";
        };
    }

    /**
     * readKnowledgeFile（读取知识文件）用于用 UTF-8（统一字符编码）读取 Markdown（标记文档）。
     *
     * @param path 知识文件路径
     * @return 文件正文
     */
    private String readKnowledgeFile(Path path) {
        // 判断知识文件是否存在，避免后续读取时报错信息不清楚。
        if (!Files.exists(path)) {
            // 抛出非法状态异常，说明知识库配置或文件缺失。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文件不存在：" + path.toAbsolutePath());
        }
        try {
            // 使用 UTF-8（统一字符编码）读取 Markdown（标记文档）文件内容。
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            // 把受检异常包装为运行时异常，交给上层统一处理。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "读取知识文件失败：" + path.toAbsolutePath(), exception);
        }
    }

    /**
     * parseBulletsUnderHeading（解析标题下的列表）用于读取指定二级标题下面的 Markdown（标记文档）项目符号。
     *
     * @param markdown Markdown（标记文档）正文
     * @param heading 二级标题名称
     * @return 项目符号内容列表
     */
    private List<String> parseBulletsUnderHeading(String markdown, String heading) {
        // 创建列表保存解析出的项目符号内容。
        List<String> bullets = new ArrayList<>();
        // 标记解析器当前是否进入目标二级标题区域。
        boolean inTargetSection = false;
        // 按行遍历 Markdown（标记文档）正文。
        for (String line : markdown.split("\\R")) {
            // 去掉当前行首尾空白，方便判断标题和项目符号。
            String trimmedLine = line.trim();
            // 如果遇到二级标题，更新当前所在区域。
            if (trimmedLine.startsWith("## ")) {
                // 判断当前二级标题是否就是目标标题。
                inTargetSection = trimmedLine.equals("## " + heading);
                // 处理完标题行后进入下一行。
                continue;
            }
            // 如果已经进入目标区域，并且当前行是项目符号，就提取项目内容。
            if (inTargetSection && trimmedLine.startsWith("- ")) {
                // 去掉 Markdown（标记文档）项目符号前缀。
                bullets.add(trimmedLine.substring(2));
            }
        }
        // 返回解析出的项目符号列表。
        return bullets;
    }
}

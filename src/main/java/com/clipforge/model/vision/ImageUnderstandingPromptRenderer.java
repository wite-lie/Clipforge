package com.clipforge.model.vision;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * ImageUnderstandingPromptRenderer（图片理解提示词渲染器）。
 *
 * <p>该组件从 Prompt（提示词）文件读取模板，并把理解指令和图片素材清单渲染成真实 VLM（视觉语言模型）调用上下文。
 */
@Component
@RequiredArgsConstructor
public class ImageUnderstandingPromptRenderer {

    /** promptProperties（提示词配置属性）用于读取 system/user（系统/用户）提示词文件路径。 */
    private final ImageUnderstandingPromptProperties promptProperties;

    /** resourceLoader（资源加载器）用于从 classpath（类路径）读取 Prompt（提示词）文件。 */
    private final ResourceLoader resourceLoader;

    /**
     * render（渲染）用于生成一次图片理解调用的提示词。
     *
     * @param request 视觉模型请求
     * @return 已渲染图片理解提示词
     */
    public RenderedImageUnderstandingPrompt render(VisionModelRequest request) {
        // 读取系统提示词模板。
        String systemTemplate = readResource(promptProperties.getSystemPath());
        // 读取用户提示词模板。
        String userTemplate = readResource(promptProperties.getUserPath());
        // 把用户模板中的变量替换为动态输入。
        String userPrompt = userTemplate
                // 替换理解指令。
                .replace("{{instruction}}", defaultText(request.getInstruction(), "请面向短视频创作规划理解图片。"))
                // 替换图片素材清单。
                .replace("{{materials}}", renderMaterials(request.getMaterials()));
        // 返回渲染后的提示词对象。
        return RenderedImageUnderstandingPrompt.builder()
                // 设置系统提示词。
                .systemPrompt(systemTemplate)
                // 设置用户提示词。
                .userPrompt(userPrompt)
                // 构建提示词。
                .build();
    }

    /**
     * renderRepair（渲染修复提示词）用于把不合法的图片理解输出修复为严格 JSON（对象表示格式）。
     *
     * @param errorMessage 解析错误消息
     * @param rawModelOutput 原始模型输出
     * @return 已渲染图片理解修复提示词
     */
    public RenderedImageUnderstandingPrompt renderRepair(String errorMessage, String rawModelOutput) {
        // 读取修复系统提示词模板。
        String systemTemplate = readResource(promptProperties.getRepairSystemPath());
        // 读取修复用户提示词模板。
        String userTemplate = readResource(promptProperties.getRepairUserPath());
        // 把修复用户模板中的变量替换为动态输入。
        String userPrompt = userTemplate
                // 替换解析错误消息。
                .replace("{{errorMessage}}", sanitizeLongText(errorMessage))
                // 替换原始模型输出。
                .replace("{{rawModelOutput}}", sanitizeLongText(rawModelOutput));
        // 返回渲染后的修复提示词对象。
        return RenderedImageUnderstandingPrompt.builder()
                // 设置系统提示词。
                .systemPrompt(systemTemplate)
                // 设置用户提示词。
                .userPrompt(userPrompt)
                // 构建提示词。
                .build();
    }

    /**
     * renderMaterials（渲染素材清单）用于把图片素材元数据转成适合写入 Prompt（提示词）的文本。
     *
     * @param materials 图片素材列表
     * @return 素材清单文本
     */
    private String renderMaterials(List<VisionModelRequestMaterial> materials) {
        // 如果素材为空，则返回明确提示。
        if (materials == null || materials.isEmpty()) {
            // 返回空素材说明。
            return "当前没有图片素材。";
        }
        // 创建字符串构建器。
        StringBuilder builder = new StringBuilder();
        // 遍历素材列表。
        for (int index = 0; index < materials.size(); index++) {
            // 读取当前素材。
            VisionModelRequestMaterial material = materials.get(index);
            // 追加素材序号。
            builder.append(index + 1).append(". ");
            // 追加素材编号。
            builder.append("materialId（素材编号）：").append(defaultText(material.getMaterialId(), "未知")).append("\n");
            // 追加原始文件名。
            builder.append("   originalFilename（原始文件名）：").append(defaultText(material.getOriginalFilename(), "未知")).append("\n");
            // 追加文件路径。
            builder.append("   filePath（文件路径）：").append(defaultText(material.getFilePath(), "未知")).append("\n");
            // 追加内容类型。
            builder.append("   contentType（内容类型）：").append(defaultText(material.getContentType(), "未知")).append("\n");
            // 追加文件大小。
            builder.append("   fileSize（文件大小）：").append(material.getFileSize() == null ? "未知" : material.getFileSize()).append("\n");
        }
        // 返回素材清单文本。
        return builder.toString();
    }

    /**
     * readResource（读取资源）用于按路径读取 Prompt（提示词）文件内容。
     *
     * @param path 资源路径
     * @return 文件内容
     */
    private String readResource(String path) {
        try {
            // 根据路径加载资源。
            Resource resource = resourceLoader.getResource(path);
            // 如果资源不存在，抛出配置错误。
            if (!resource.exists()) {
                // 抛出模型配置错误。
                throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "图片理解 Prompt（提示词）文件不存在：" + path);
            }
            // 读取资源字节并按 UTF-8（统一字符编码）转为字符串。
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            // 读取失败时包装为配置错误。
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "读取图片理解 Prompt（提示词）文件失败：" + path, exception);
        }
    }

    /**
     * defaultText（默认文本）用于处理空字符串。
     *
     * @param value 原始文本
     * @param defaultValue 默认文本
     * @return 最终文本
     */
    private String defaultText(String value, String defaultValue) {
        // 如果原始文本为空，则返回默认文本。
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * sanitizeLongText（清洗长文本）用于避免原始模型输出破坏修复 Prompt（提示词）结构。
     *
     * @param value 原始文本
     * @return 清洗后的文本
     */
    private String sanitizeLongText(String value) {
        // 空文本统一转换为空字符串。
        String safeValue = defaultText(value, "");
        // 替换 Markdown（标记文档）分隔符，避免破坏提示词结构。
        safeValue = safeValue.replace("---", "___");
        // 如果文本过长，则截断，避免修复调用上下文失控。
        if (safeValue.length() > 6000) {
            // 返回截断后的文本。
            return safeValue.substring(0, 6000) + "...";
        }
        // 返回清洗后的文本。
        return safeValue;
    }
}

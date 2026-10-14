package com.clipforge.creation.generator;

import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CreationPlanJsonParser（创作方案 JSON 解析器）。
 *
 * <p>该组件负责把 LLM（大语言模型）返回的文本清理并解析为 CreationPlanResult（创作方案结果）。
 */
@Component
@RequiredArgsConstructor
public class CreationPlanJsonParser {

    /** objectMapper（对象映射器）用于执行 JSON（对象表示格式）和 Java（爪哇语言）对象之间的转换。 */
    private final ObjectMapper objectMapper;

    /**
     * parse（解析）用于把模型文本响应转换为结构化创作方案。
     *
     * @param rawContent 模型原始响应文本
     * @return 创作方案结果
     */
    public CreationPlanResult parse(String rawContent) {
        // 清理模型输出，去掉可能出现的 Markdown（标记文档）代码围栏和额外解释文本。
        String jsonContent = extractJsonObject(rawContent);
        try {
            // 使用 Jackson（JSON 处理工具）把 JSON（对象表示格式）转换为业务结果对象。
            return objectMapper.readValue(jsonContent, CreationPlanResult.class);
        } catch (JsonProcessingException exception) {
            // 解析失败时抛出业务异常，方便上层统一处理和记录 Trace（执行链路追踪）。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型输出不是合法的创作方案 JSON：" + exception.getOriginalMessage(), exception);
        }
    }

    /**
     * extractJsonObject（提取 JSON 对象）用于从模型输出中截取第一个完整 JSON（对象表示格式）对象。
     *
     * @param rawContent 模型原始响应文本
     * @return JSON（对象表示格式）对象文本
     */
    private String extractJsonObject(String rawContent) {
        // 如果原始响应为空，直接抛出解析异常。
        if (rawContent == null || rawContent.isBlank()) {
            // 抛出模型输出解析错误。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型输出为空，无法解析创作方案 JSON");
        }
        // 去掉首尾空白字符。
        String content = rawContent.strip();
        // 去掉常见的 Markdown（标记文档）JSON 代码围栏开头。
        content = content.replaceFirst("^```json\\s*", "");
        // 去掉常见的 Markdown（标记文档）普通代码围栏开头。
        content = content.replaceFirst("^```\\s*", "");
        // 去掉常见的 Markdown（标记文档）代码围栏结尾。
        content = content.replaceFirst("\\s*```$", "");
        // 查找第一个左大括号。
        int startIndex = content.indexOf('{');
        // 查找最后一个右大括号。
        int endIndex = content.lastIndexOf('}');
        // 如果没有找到完整对象边界，说明输出不可解析。
        if (startIndex < 0 || endIndex < startIndex) {
            // 抛出模型输出解析错误。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型输出中没有找到完整 JSON 对象");
        }
        // 返回 JSON（对象表示格式）对象文本。
        return content.substring(startIndex, endIndex + 1);
    }
}

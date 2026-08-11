package com.clipforge.model.vision;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ImageUnderstandingJsonParser（图片理解 JSON 解析器）。
 *
 * <p>该组件负责把 VLM（视觉语言模型）返回的文本清理并解析为 ImageUnderstandingResult（图片理解结果）列表。
 */
@Component
@RequiredArgsConstructor
public class ImageUnderstandingJsonParser {

    /** objectMapper（对象映射器）用于执行 JSON（对象表示格式）和 Java（爪哇语言）对象之间的转换。 */
    private final ObjectMapper objectMapper;

    /**
     * parseResults（解析结果列表）用于从模型原始输出中读取图片理解结果。
     *
     * @param rawContent 模型原始输出
     * @return 图片理解结果列表
     */
    public List<ImageUnderstandingResult> parseResults(String rawContent) {
        // 提取 JSON（对象表示格式）文本。
        String jsonContent = extractJson(rawContent);
        try {
            // 解析为 Jackson（JSON 处理工具）树。
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            // 如果根节点是数组，直接按结果数组解析。
            if (rootNode.isArray()) {
                // 返回数组解析结果。
                return parseArray(rootNode);
            }
            // 如果根节点包含 results（结果列表）字段，则解析该字段。
            if (rootNode.has("results") && rootNode.get("results").isArray()) {
                // 返回 results（结果列表）解析结果。
                return parseArray(rootNode.get("results"));
            }
            // 如果根节点是单个对象，则按单图结果解析。
            if (rootNode.isObject()) {
                // 返回单对象解析结果。
                return List.of(objectMapper.treeToValue(rootNode, ImageUnderstandingResult.class));
            }
            // 其他结构不可接受。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "图片理解模型输出 JSON 结构不支持");
        } catch (JsonProcessingException exception) {
            // 解析失败时抛出业务异常。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "图片理解模型输出不是合法 JSON：" + exception.getOriginalMessage(), exception);
        }
    }

    /**
     * parseArray（解析数组）用于把 JSON 数组转换为图片理解结果列表。
     *
     * @param arrayNode JSON 数组节点
     * @return 图片理解结果列表
     * @throws JsonProcessingException JSON 转换异常
     */
    private List<ImageUnderstandingResult> parseArray(JsonNode arrayNode) throws JsonProcessingException {
        // 创建结果列表。
        List<ImageUnderstandingResult> results = new ArrayList<>();
        // 遍历数组节点。
        for (JsonNode itemNode : arrayNode) {
            // 把每个对象转换为图片理解结果。
            results.add(objectMapper.treeToValue(itemNode, ImageUnderstandingResult.class));
        }
        // 返回结果列表。
        return results;
    }

    /**
     * extractJson（提取 JSON）用于去掉模型可能附带的 Markdown（标记文档）代码围栏和说明文字。
     *
     * @param rawContent 模型原始输出
     * @return JSON 文本
     */
    private String extractJson(String rawContent) {
        // 如果原始响应为空，直接抛出解析异常。
        if (rawContent == null || rawContent.isBlank()) {
            // 抛出模型输出解析错误。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "图片理解模型输出为空");
        }
        // 去掉首尾空白。
        String content = rawContent.strip();
        // 去掉常见 Markdown JSON 代码围栏开头。
        content = content.replaceFirst("^```json\\s*", "");
        // 去掉常见 Markdown 普通代码围栏开头。
        content = content.replaceFirst("^```\\s*", "");
        // 去掉常见 Markdown 代码围栏结尾。
        content = content.replaceFirst("\\s*```$", "");
        // 查找第一个对象或数组开始位置。
        int objectStart = content.indexOf('{');
        // 查找第一个数组开始位置。
        int arrayStart = content.indexOf('[');
        // 计算 JSON 开始位置。
        int startIndex = chooseStartIndex(objectStart, arrayStart);
        // 查找最后一个对象结束位置。
        int objectEnd = content.lastIndexOf('}');
        // 查找最后一个数组结束位置。
        int arrayEnd = content.lastIndexOf(']');
        // 计算 JSON 结束位置。
        int endIndex = Math.max(objectEnd, arrayEnd);
        // 如果没有找到完整边界，说明输出不可解析。
        if (startIndex < 0 || endIndex < startIndex) {
            // 抛出模型输出解析错误。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "图片理解模型输出中没有找到完整 JSON");
        }
        // 返回 JSON 文本。
        return content.substring(startIndex, endIndex + 1);
    }

    /**
     * chooseStartIndex（选择开始位置）用于在对象和数组起点之间选择更靠前的有效位置。
     *
     * @param objectStart 对象开始位置
     * @param arrayStart 数组开始位置
     * @return JSON 开始位置
     */
    private int chooseStartIndex(int objectStart, int arrayStart) {
        // 如果对象起点不存在，返回数组起点。
        if (objectStart < 0) {
            // 返回数组起点。
            return arrayStart;
        }
        // 如果数组起点不存在，返回对象起点。
        if (arrayStart < 0) {
            // 返回对象起点。
            return objectStart;
        }
        // 两者都存在时返回更靠前的位置。
        return Math.min(objectStart, arrayStart);
    }
}

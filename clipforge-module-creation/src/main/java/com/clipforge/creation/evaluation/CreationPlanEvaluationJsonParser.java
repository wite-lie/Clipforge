package com.clipforge.creation.evaluation;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CreationPlanEvaluationJsonParser（创作方案评估 JSON 解析器）。
 *
 * <p>该组件负责把 LLM-as-Judge（大模型裁判）返回的文本解析为 CreationPlanEvaluationResult（创作方案评估结果）。
 */
@Component
@RequiredArgsConstructor
public class CreationPlanEvaluationJsonParser {

    /** REQUIRED_DIMENSIONS（必需评估维度）定义当前系统要求模型必须返回的六个维度。 */
    private static final List<String> REQUIRED_DIMENSIONS = List.of("hookStrength", "platformFit", "materialRelevance", "scriptCompleteness", "executionFeasibility", "commercialValue");

    /** objectMapper（对象映射器）用于执行 JSON（对象表示格式）和 Java（爪哇语言）对象之间的转换。 */
    private final ObjectMapper objectMapper;

    /**
     * parse（解析）用于把模型输出解析为结构化评估结果。
     *
     * @param rawContent 模型原始响应文本
     * @return 创作方案评估结果
     */
    public CreationPlanEvaluationResult parse(String rawContent) {
        // 提取 JSON（对象表示格式）对象文本。
        String jsonContent = extractJsonObject(rawContent);
        try {
            // 使用 Jackson（JSON 处理工具）转换为评估结果对象。
            CreationPlanEvaluationResult result = objectMapper.readValue(jsonContent, CreationPlanEvaluationResult.class);
            // 校验评估结果，避免坏结构进入后续 Reflection（反思修正）。
            validate(result);
            // 返回评估结果。
            return result;
        } catch (JsonProcessingException exception) {
            // 解析失败时抛出模型输出解析异常。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型输出不是合法的创作方案评估 JSON：" + exception.getOriginalMessage(), exception);
        }
    }

    /**
     * validate（校验）用于确认评估结果满足系统契约。
     *
     * @param result 创作方案评估结果
     */
    private void validate(CreationPlanEvaluationResult result) {
        // 如果评估结果为空，则抛出异常。
        if (result == null) {
            // 抛出模型输出解析异常。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型输出评估结果为空");
        }
        // 校验综合评分范围。
        validateScore("overallScore", result.getOverallScore());
        // 校验维度评分列表。
        if (result.getDimensionScores() == null || result.getDimensionScores().size() != REQUIRED_DIMENSIONS.size()) {
            // 抛出模型输出解析异常。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型评估结果必须包含六个维度评分");
        }
        // 遍历必需维度，确认每个维度都存在。
        for (String requiredDimension : REQUIRED_DIMENSIONS) {
            // 判断当前维度是否存在。
            boolean exists = result.getDimensionScores().stream().anyMatch(score -> requiredDimension.equals(score.getDimension()));
            // 如果维度缺失，则抛出异常。
            if (!exists) {
                // 抛出模型输出解析异常。
                throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型评估结果缺少维度：" + requiredDimension);
            }
        }
        // 校验每个维度的分数和原因。
        for (EvaluationDimensionScore dimensionScore : result.getDimensionScores()) {
            // 校验维度分数范围。
            validateScore(dimensionScore.getDimension(), dimensionScore.getScore());
            // 校验评分原因。
            if (dimensionScore.getReason() == null || dimensionScore.getReason().isBlank()) {
                // 抛出模型输出解析异常。
                throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型评估结果维度原因不能为空：" + dimensionScore.getDimension());
            }
        }
        // 如果需要修正，则修正建议不能为空。
        if (result.isRevisionRequired() && (result.getRevisionAdvice() == null || result.getRevisionAdvice().isEmpty())) {
            // 抛出模型输出解析异常。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型评估结果要求修正时必须给出修正建议");
        }
    }

    /**
     * validateScore（校验分数）用于确认分数在 0 到 100 之间。
     *
     * @param fieldName 字段名称
     * @param score 分数
     */
    private void validateScore(String fieldName, int score) {
        // 如果分数越界，则抛出异常。
        if (score < 0 || score > 100) {
            // 抛出模型输出解析异常。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型评估分数越界：" + fieldName + "=" + score);
        }
    }

    /**
     * extractJsonObject（提取 JSON 对象）用于从模型输出中截取第一个完整 JSON（对象表示格式）对象。
     *
     * @param rawContent 模型原始响应文本
     * @return JSON（对象表示格式）对象文本
     */
    private String extractJsonObject(String rawContent) {
        // 如果原始响应为空，则抛出解析异常。
        if (rawContent == null || rawContent.isBlank()) {
            // 抛出模型输出解析错误。
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型输出为空，无法解析创作方案评估 JSON");
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
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "模型输出中没有找到完整评估 JSON 对象");
        }
        // 返回 JSON（对象表示格式）对象文本。
        return content.substring(startIndex, endIndex + 1);
    }
}

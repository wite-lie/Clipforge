package com.clipforge.ai.infrastructure.output;

import org.springframework.stereotype.Component;

/**
 * ModelOutputTruncationDetector（模型输出截断识别器）。
 *
 * <p>该组件使用启发式规则识别 LLM（大语言模型）或 VLM（视觉语言模型）输出是否疑似被截断。
 */
@Component
public class ModelOutputTruncationDetector {

    /** jsonCompletenessChecker（JSON 完整性检查器）用于判断结构化输出外形是否闭合。 */
    private final JsonCompletenessChecker jsonCompletenessChecker;

    /**
     * ModelOutputTruncationDetector（模型输出截断识别器）构造函数。
     *
     * @param jsonCompletenessChecker JSON 完整性检查器
     */
    public ModelOutputTruncationDetector(JsonCompletenessChecker jsonCompletenessChecker) {
        this.jsonCompletenessChecker = jsonCompletenessChecker;
    }

    /**
     * detect（识别）用于判断模型输出是否疑似截断。
     *
     * @param rawContent 原始模型输出
     * @param finishReason 模型结束原因
     * @param maxOutputTokens 最大输出 Token（词元）
     * @return 截断识别结果
     */
    public ModelOutputTruncationResult detect(String rawContent, String finishReason, Integer maxOutputTokens) {
        int responseLength = rawContent == null ? 0 : rawContent.length();
        if (rawContent == null || rawContent.isBlank()) {
            return result(false, false, ModelOutputIssueType.EMPTY_OUTPUT, 0.4, "模型输出为空", responseLength);
        }
        if (isLengthFinishReason(finishReason)) {
            return result(true, true, ModelOutputIssueType.TRUNCATED_BY_FINISH_REASON, 0.95, "模型结束原因表示达到最大输出长度", responseLength);
        }
        JsonCompletenessResult completenessResult = jsonCompletenessChecker.check(rawContent);
        if (!completenessResult.isComplete() && isTruncationShape(completenessResult.getIssueType())) {
            return result(true, true, completenessResult.getIssueType(), 0.85, completenessResult.getReason(), responseLength);
        }
        if (hasSuspiciousTail(rawContent)) {
            return result(true, true, ModelOutputIssueType.SUSPICIOUS_TAIL, 0.7, "模型输出末尾停在疑似未完成位置", responseLength);
        }
        if (maxOutputTokens != null && maxOutputTokens > 0 && responseLength >= maxOutputTokens * 3) {
            return result(true, true, ModelOutputIssueType.SUSPICIOUS_TAIL, 0.55, "输出长度接近最大输出 Token 的粗略字符上限", responseLength);
        }
        if (!completenessResult.isComplete()) {
            return result(false, true, completenessResult.getIssueType(), 0.5, completenessResult.getReason(), responseLength);
        }
        return result(false, false, ModelOutputIssueType.NONE, 0.0, "未发现明显截断", responseLength);
    }

    /**
     * isLengthFinishReason（是否长度结束原因）用于兼容不同供应商的结束原因命名。
     */
    private boolean isLengthFinishReason(String finishReason) {
        if (finishReason == null) {
            return false;
        }
        String normalized = finishReason.trim().toLowerCase();
        return "length".equals(normalized)
                || "max_tokens".equals(normalized)
                || "max_output_tokens".equals(normalized)
                || "max_completion_tokens".equals(normalized);
    }

    /**
     * isTruncationShape（是否截断形态）用于把 JSON 未闭合类问题判定为可修复截断。
     */
    private boolean isTruncationShape(ModelOutputIssueType issueType) {
        return issueType == ModelOutputIssueType.UNCLOSED_JSON_OBJECT
                || issueType == ModelOutputIssueType.UNCLOSED_JSON_ARRAY
                || issueType == ModelOutputIssueType.UNCLOSED_JSON_STRING;
    }

    /**
     * hasSuspiciousTail（是否存在可疑尾部）用于识别停在逗号、冒号、半个字符串等位置的输出。
     */
    private boolean hasSuspiciousTail(String rawContent) {
        String content = rawContent.strip();
        if (content.isEmpty()) {
            return false;
        }
        char last = content.charAt(content.length() - 1);
        return last == ',' || last == ':' || last == '"' || last == '[' || last == '{';
    }

    /**
     * result（结果）用于统一构建截断识别结果。
     */
    private ModelOutputTruncationResult result(boolean truncated, boolean repairable, ModelOutputIssueType issueType,
                                               double confidence, String reason, int responseLength) {
        return ModelOutputTruncationResult.builder()
                .truncated(truncated)
                .repairable(repairable)
                .issueType(issueType)
                .confidence(confidence)
                .reason(reason)
                .responseLength(responseLength)
                .build();
    }
}

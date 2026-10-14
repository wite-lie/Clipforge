package com.clipforge.creation.article;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.contract.creation.ArticleCreationPlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * ArticleCreationPlanJsonParser（图文方案 JSON 解析器）。
 */
@Component
public class ArticleCreationPlanJsonParser {

    /** objectMapper（对象映射器）用于解析模型输出 JSON。 */
    private final ObjectMapper objectMapper;

    public ArticleCreationPlanJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * parse（解析）用于把模型输出文本解析为图文方案。
     *
     * @param content 模型输出内容
     * @return 图文方案
     */
    public ArticleCreationPlan parse(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "图文方案模型输出为空");
        }
        try {
            return objectMapper.readValue(clean(content), ArticleCreationPlan.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "图文方案 JSON 解析失败", exception);
        }
    }

    private String clean(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }
}

package com.clipforge.prompt.token;

import java.util.List;

/**
 * PromptTokenEstimator（提示词词元估算器）。
 *
 * <p>该接口用于在调用 LLM（大语言模型）或 VLM（视觉语言模型）前，对 Prompt（提示词）进行本地大致 Token（词元）估算。
 */
public interface PromptTokenEstimator {

    /**
     * estimateText（估算文本）用于估算单段文本大致 Token（词元）数量。
     *
     * @param text 文本
     * @return 估算 Token（词元）数量
     */
    int estimateText(String text);

    /**
     * estimate（估算）用于按片段估算一次 Prompt（提示词）的总 Token（词元）和分段明细。
     *
     * @param sections 提示词片段列表
     * @return 提示词词元估算结果
     */
    PromptTokenEstimateResult estimate(List<PromptSection> sections);
}

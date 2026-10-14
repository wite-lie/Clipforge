package com.clipforge.prompt.token;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PromptTokenSectionEstimate（提示词片段词元估算）。
 *
 * <p>该对象保存单个 PromptSection（提示词片段）的字符数量、估算 Token（词元）数量和占比。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTokenSectionEstimate {

    /** type（类型）表示提示词片段来源。 */
    private PromptSectionType type;

    /** name（名称）表示人类可读的片段名称。 */
    private String name;

    /** characterCount（字符数）表示原始文本字符数量。 */
    private Integer characterCount;

    /** estimatedTokens（估算词元数）表示本片段大致占用 Token（词元）数量。 */
    private Integer estimatedTokens;

    /** ratio（占比）表示本片段在真实总 Prompt（提示词）估算 Token 中的占比。 */
    private BigDecimal ratio;

    /** includedInTotal（是否计入总数）表示该片段是否参与真实总数统计。 */
    private Boolean includedInTotal;
}

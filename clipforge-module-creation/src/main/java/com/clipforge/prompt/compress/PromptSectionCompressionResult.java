package com.clipforge.prompt.compress;

import com.clipforge.prompt.token.PromptSectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PromptSectionCompressionResult（提示词片段压缩结果）。
 *
 * <p>该对象记录单个上下文片段压缩前后的字符数、Token（词元）估算和压缩动作。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptSectionCompressionResult {

    /** sectionType（片段类型）表示被处理的上下文来源。 */
    private PromptSectionType sectionType;

    /** action（压缩动作）表示本次如何处理该片段。 */
    private PromptCompressionAction action;

    /** beforeCharacters（压缩前字符数）表示原始文本长度。 */
    private Integer beforeCharacters;

    /** afterCharacters（压缩后字符数）表示处理后文本长度。 */
    private Integer afterCharacters;

    /** beforeEstimatedTokens（压缩前估算词元）表示原始文本估算 Token（词元）。 */
    private Integer beforeEstimatedTokens;

    /** afterEstimatedTokens（压缩后估算词元）表示处理后文本估算 Token（词元）。 */
    private Integer afterEstimatedTokens;

    /** reason（原因）表示触发裁剪的原因。 */
    private String reason;
}

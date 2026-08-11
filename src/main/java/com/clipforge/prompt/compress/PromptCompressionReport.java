package com.clipforge.prompt.compress;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PromptCompressionReport（提示词压缩报告）。
 *
 * <p>该对象汇总一次 Prompt（提示词）压缩前后的整体变化和分段处理明细。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptCompressionReport {

    /** compressed（是否发生压缩）表示本次是否真正裁剪了内容。 */
    private Boolean compressed;

    /** reason（原因）表示触发或跳过压缩的整体原因。 */
    private String reason;

    /** beforeEstimatedTokens（压缩前估算词元）表示原始 Prompt（提示词）估算 Token（词元）。 */
    private Integer beforeEstimatedTokens;

    /** afterEstimatedTokens（压缩后估算词元）第一版在重新渲染后由生成器回填。 */
    private Integer afterEstimatedTokens;

    /** sections（片段结果）表示每个上下文来源的压缩明细。 */
    private List<PromptSectionCompressionResult> sections;
}

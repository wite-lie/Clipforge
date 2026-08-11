package com.clipforge.prompt.compress;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PromptCompressionResult（提示词压缩结果）。
 *
 * <p>该对象承载可写入 AgentContext（智能体上下文）的压缩覆盖文本和压缩报告。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptCompressionResult {

    /** compressed（是否发生压缩）表示本次是否裁剪了任何片段。 */
    private Boolean compressed;

    /** overrides（覆盖文本）表示渲染器应优先使用的压缩后片段文本。 */
    private Map<String, String> overrides;

    /** report（压缩报告）表示本次压缩明细。 */
    private PromptCompressionReport report;
}

package com.clipforge.tool;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CreativeKnowledge（创作知识）。
 *
 * <p>该对象表示从创作知识库中检索到的平台规则、脚本建议和评估提示。
 */
@Getter
@RequiredArgsConstructor
public class CreativeKnowledge {

    /** platform（平台）表示当前知识适配的目标平台。 */
    private final String platform;

    /** platformRules（平台规则）表示平台侧创作建议。 */
    private final List<String> platformRules;

    /** scriptPatterns（脚本结构）表示适合当前场景的内容组织方式。 */
    private final List<String> scriptPatterns;

    /** evaluationHints（评估提示）表示后续评分时需要关注的维度。 */
    private final List<String> evaluationHints;
}

package com.clipforge.knowledge.evidence;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RagEvidenceProperties（RAG 证据配置）。
 *
 * <p>该配置控制 RAG（检索增强生成）证据进入 Prompt（提示词）前的数量、长度和预算保护策略。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rag.evidence")
public class RagEvidenceProperties {

    /** maxEvidenceCount（最大证据数）表示最终进入 Prompt（提示词）的证据条数，默认与 Rerank（重排序）TopN 保持一致。 */
    private int maxEvidenceCount = 5;

    /** maxPromptContextChars（最大证据上下文字数）表示 RAG（检索增强生成）证据区的总字符预算。 */
    private int maxPromptContextChars = 8000;

    /** minContentChars（最小内容字数）表示过短切片不进入 Prompt（提示词），避免无效证据污染上下文。 */
    private int minContentChars = 8;

    /** maxEvidencePerDocument（单文档最大证据数）用于避免同一个文档占满全部证据位置。 */
    private int maxEvidencePerDocument = 3;

    /** truncateSingleEvidenceWhenNecessary（必要时截断单条证据）表示减少证据条数后仍超预算时是否允许截断正文。 */
    private boolean truncateSingleEvidenceWhenNecessary = true;

    /** insufficientEvidenceMarkerEnabled（启用证据不足提示）表示证据不足时是否在 Prompt（提示词）中加入保守生成提示。 */
    private boolean insufficientEvidenceMarkerEnabled = true;
}

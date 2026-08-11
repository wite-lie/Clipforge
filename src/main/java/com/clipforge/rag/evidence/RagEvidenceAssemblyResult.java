package com.clipforge.rag.evidence;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RagEvidenceAssemblyResult（RAG 证据组装结果）。
 *
 * <p>该对象同时返回 Prompt（提示词）上下文、进入 Prompt 的证据和被过滤证据，便于主流程注入和 Trace（执行链路追踪）观察。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagEvidenceAssemblyResult {

    /** promptContext（提示词上下文）表示已经渲染好的 RAG（检索增强生成）证据文本。 */
    private String promptContext;

    /** promptEvidences（提示词证据）表示最终进入 Prompt（提示词）的证据列表。 */
    private List<RagEvidence> promptEvidences;

    /** filteredEvidences（被过滤证据）表示因重复、空内容或预算原因没有进入 Prompt（提示词）的证据列表。 */
    private List<RagEvidence> filteredEvidences;

    /** budgetProtectionTriggered（是否触发预算保护）表示是否因为证据总长度过长而减少证据条数或截断内容。 */
    private boolean budgetProtectionTriggered;

    /** originalChunkCount（原始切片数量）表示进入证据组装器前的切片数量。 */
    private int originalChunkCount;

    /** promptEvidenceCount（提示词证据数量）表示最终进入 Prompt（提示词）的证据数量。 */
    private int promptEvidenceCount;

    /** promptContextChars（提示词上下文字数）表示最终 RAG（检索增强生成）证据区字符数。 */
    private int promptContextChars;
}

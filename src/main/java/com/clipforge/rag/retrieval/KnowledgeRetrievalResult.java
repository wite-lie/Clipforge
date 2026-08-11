package com.clipforge.rag.retrieval;

import com.clipforge.rag.evidence.RagEvidence;
import com.clipforge.rag.evidence.RagEvidenceAssemblyResult;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeRetrievalResult（知识检索结果）。
 *
 * <p>该对象表示一次 RAG（检索增强生成）检索的完整结果，包括命中切片、证据组装结果、是否降级和可直接注入 Prompt（提示词）的上下文。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRetrievalResult {

    /** query（查询文本）表示本次检索使用的文本。 */
    private String query;

    /** topK（前 K 个）表示本次检索最多返回多少条结果。 */
    private Integer topK;

    /** source（来源）表示最终结果主来源。 */
    private KnowledgeRetrievalSource source;

    /** fallbackUsed（是否使用降级）表示本次是否回退到静态知识库。 */
    private boolean fallbackUsed;

    /** fallbackReason（降级原因）用于 Trace（执行链路追踪）和调试复盘。 */
    private String fallbackReason;

    /** chunks（知识切片列表）表示最终返回的候选知识。 */
    private List<KnowledgeRetrievedChunk> chunks;

    /** promptEvidences（提示词证据）表示最终进入 Prompt（提示词）的 RAG（检索增强生成）证据。 */
    private List<RagEvidence> promptEvidences;

    /** evidenceAssemblyResult（证据组装结果）表示证据过滤、预算保护和渲染的完整过程摘要。 */
    private RagEvidenceAssemblyResult evidenceAssemblyResult;

    /** promptContext（提示词上下文）表示把检索结果拼接成可注入 LLM（大语言模型）的文本。 */
    private String promptContext;
}

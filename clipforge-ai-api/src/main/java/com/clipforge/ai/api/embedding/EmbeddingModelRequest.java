package com.clipforge.ai.api.embedding;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * EmbeddingModelRequest（嵌入模型请求）。
 *
 * <p>该对象用于承载一次文本向量化请求，后续主要服务于 KnowledgeChunk（知识切片）的 Embedding（嵌入）生成。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingModelRequest {

    /** runId（运行编号）用于后续 Trace（执行链路追踪）和模型用量记录，当前 RAG 测试阶段可以为空。 */
    private String runId;

    /** projectId（项目编号）用于后续按项目统计 RAG（检索增强生成）模型成本。 */
    private String projectId;

    /** conversationId（会话编号）用于后续按会话统计 RAG（检索增强生成）模型成本。 */
    private String conversationId;

    /** purpose（调用用途）用于区分知识切片向量化、查询向量化等场景。 */
    private String purpose;

    /** texts（文本列表）表示本次需要生成向量的文本内容。 */
    private List<String> texts;
}

package com.clipforge.rag.retrieval;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeRetrievalRequest（知识检索请求）。
 *
 * <p>该对象用于承载一次 RAG（检索增强生成）检索需要的输入，后续创作 Workflow（工作流）会把用户目标、平台、素材摘要和知识库编号传入这里。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRetrievalRequest {

    /** query（查询文本）表示本次要检索的自然语言问题或创作目标。 */
    private String query;

    /** platform（目标平台）用于静态降级时读取对应平台知识，也可供后续混合检索做过滤。 */
    private String platform;

    /** knowledgeBaseIds（知识库编号列表）表示向量检索范围；为空且未开启全库检索时直接走静态降级。 */
    private List<String> knowledgeBaseIds;

    /** searchAllKnowledgeBases（检索全部知识库）表示不限制 knowledgeBaseIds（知识库编号列表），在全部有效切片中检索。 */
    private Boolean searchAllKnowledgeBases;

    /** topK（前 K 个）表示最多返回多少条候选知识；为空时使用配置默认值。 */
    private Integer topK;

    /** staticFallbackEnabled（启用静态降级）表示本次请求是否允许回退到静态知识库；为空时使用全局配置。 */
    private Boolean staticFallbackEnabled;
}

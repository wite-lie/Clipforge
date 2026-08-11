package com.clipforge.tool.builtin;

import com.clipforge.rag.retrieval.KnowledgeRetrievalRequest;
import com.clipforge.rag.retrieval.KnowledgeRetrievalResult;
import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.definition.ToolDefinitionProvider;
import com.clipforge.tool.definition.ToolEffectType;
import com.clipforge.tool.definition.ToolInvocationPolicy;
import com.clipforge.tool.definition.ToolSchema;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RagRetrieveToolDefinitionProvider（RAG 检索工具定义提供器）。
 */
@Component
public class RagRetrieveToolDefinitionProvider implements ToolDefinitionProvider {

    @Override
    public List<ToolDefinition> definitions() {
        return List.of(ToolDefinition.builder()
                .name("rag_retrieve")
                .displayName("RAG 知识库检索")
                .description("根据用户目标、平台、素材摘要和知识库范围检索可注入创作提示词的证据")
                .effectType(ToolEffectType.READ_ONLY)
                .inputSchema(ToolSchema.of(KnowledgeRetrievalRequest.class.getName(), "查询文本、平台、知识库编号、topK 和降级开关"))
                .outputSchema(ToolSchema.of(KnowledgeRetrievalResult.class.getName(), "检索来源、候选切片、重排结果、Prompt 证据和降级信息"))
                .invocationPolicy(ToolInvocationPolicy.readOnlyLlmCallable())
                .ownerModule("rag")
                .version("v1")
                .build());
    }
}

package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FullProcessKnowledgeDocumentResponse（完整处理知识文档响应）。
 *
 * <p>该对象用于返回“解析切片 + Embedding（嵌入）向量化 + pgvector（PostgreSQL 向量扩展）入库”的端到端处理摘要。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullProcessKnowledgeDocumentResponse {

    /** processLogId（处理日志编号）用于在 cf_knowledge_document_process_log（知识文档处理日志表）中复盘本次处理。 */
    private String processLogId;

    /** document（知识文档响应）表示处理完成后的文档状态。 */
    private KnowledgeDocumentResponse document;

    /** processResponse（解析切片响应）表示解析切片阶段的摘要和切片预览。 */
    private ProcessKnowledgeDocumentResponse processResponse;

    /** chunkCount（切片数量）表示本次完整处理参与向量化的切片数量。 */
    private Integer chunkCount;

    /** embeddingCount（向量数量）表示写入 cf_knowledge_embedding（知识向量表）的有效向量数量。 */
    private Integer embeddingCount;

    /** embeddingProvider（嵌入模型供应商）表示本次使用的 Embedding（嵌入）模型供应商。 */
    private String embeddingProvider;

    /** embeddingModelName（嵌入模型名称）表示本次使用的 Embedding（嵌入）模型名称。 */
    private String embeddingModelName;

    /** embeddingDimension（嵌入向量维度）表示模型返回的向量维度。 */
    private Integer embeddingDimension;

    /** promptTokens（输入词元）表示 Embedding（嵌入）模型输入消耗。 */
    private Integer promptTokens;

    /** totalTokens（总词元）表示 Embedding（嵌入）模型总消耗。 */
    private Integer totalTokens;

    /** chunkDurationMs（切片耗时毫秒）表示解析和切片阶段总耗时。 */
    private Long chunkDurationMs;

    /** embeddingDurationMs（嵌入耗时毫秒）表示向量化和入库阶段总耗时。 */
    private Long embeddingDurationMs;

    /** totalDurationMs（总耗时毫秒）表示完整处理链路总耗时。 */
    private Long totalDurationMs;

    /** status（处理状态）表示本次完整处理是否成功。 */
    private String status;
}

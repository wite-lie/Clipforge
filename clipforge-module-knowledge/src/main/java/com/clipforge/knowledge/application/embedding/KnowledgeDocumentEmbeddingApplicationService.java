package com.clipforge.knowledge.application.embedding;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeChunkRepository;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentRepository;
import com.clipforge.knowledge.application.processing.KnowledgeDocumentStatusUseCase;
import com.clipforge.ai.api.embedding.EmbeddingModelClient;
import com.clipforge.ai.api.embedding.EmbeddingModelRequest;
import com.clipforge.ai.api.embedding.EmbeddingModelResponse;
import com.clipforge.observability.usage.ModelCallRecord;
import com.clipforge.observability.usage.ModelUsageRecorder;
import com.clipforge.knowledge.vector.VectorStore;
import com.clipforge.knowledge.vector.VectorStoreReplaceDocumentRequest;
import com.clipforge.knowledge.vector.VectorStoreSearchRequest;
import com.clipforge.knowledge.vector.VectorStoreSearchResult;
import com.clipforge.knowledge.vector.VectorStoreUpsertItem;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * KnowledgeDocumentEmbeddingApplicationService（知识文档向量化应用服务）。
 *
 * <p>该服务负责编排 Phase 7.6（阶段 7.6）的“已有切片向量化、向量入库、向量检索”链路。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDocumentEmbeddingApplicationService {

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于获取 db（数据库）Profile（配置环境）下的文档仓储。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** knowledgeChunkRepositoryProvider（知识切片仓储提供器）用于获取文档有效切片。 */
    private final ObjectProvider<KnowledgeChunkRepository> knowledgeChunkRepositoryProvider;

    /** vectorStoreProvider（向量存储提供器）用于检索 pgvector（PostgreSQL 向量扩展）数据。 */
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    /** embeddingPersistenceServiceProvider（向量化持久化服务提供器）用于短事务写入向量并更新文档状态。 */
    private final ObjectProvider<KnowledgeDocumentEmbeddingPersistenceService> embeddingPersistenceServiceProvider;

    /** knowledgeDocumentStatusUseCaseProvider（知识文档状态用例提供器）用于记录向量化失败状态。 */
    private final ObjectProvider<KnowledgeDocumentStatusUseCase> knowledgeDocumentStatusUseCaseProvider;

    /** embeddingModelClient（嵌入模型客户端）用于把切片文本或查询文本转换为向量。 */
    private final EmbeddingModelClient embeddingModelClient;

    /** modelUsageRecorder（模型用量记录器）用于记录 Embedding（嵌入）模型 Token（词元）成本。 */
    private final ModelUsageRecorder modelUsageRecorder;

    /**
     * embedDocumentChunks（向量化文档切片）用于把指定文档下全部有效切片写入向量表。
     *
     * @param documentId 知识文档编号
     * @return 文档向量化响应
     */
    public KnowledgeDocumentEmbeddingResponse embedDocumentChunks(String documentId) {
        // 查询文档，确认文档存在且没有被逻辑删除。
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        try {
            // 查询该文档下全部有效切片。
            List<KnowledgeChunkEntity> chunks = requiredKnowledgeChunkRepository().listByDocumentId(documentId);
            // 如果没有切片，说明文档尚未完成 Phase 7.4（阶段 7.4）处理。
            if (chunks == null || chunks.isEmpty()) {
                // 抛出业务异常。
                throw new BusinessException(ErrorCode.BAD_REQUEST, "文档没有有效切片，请先完成文档解析和切片，documentId：" + documentId);
            }
            // 提取切片正文，模型调用放在数据库事务外，避免长事务占用连接和锁。
            List<String> texts = chunks.stream()
                    // 读取切片正文。
                    .map(KnowledgeChunkEntity::getContent)
                    // 收集为列表。
                    .toList();
            // 生成模型调用运行编号，便于后续成本复盘。
            String runId = newId("rag_doc_emb");
            // 记录模型调用开始时间。
            long embeddingStart = System.currentTimeMillis();
            // 构造嵌入模型请求。
            EmbeddingModelRequest embeddingRequest = EmbeddingModelRequest.builder()
                    // 设置运行编号。
                    .runId(runId)
                    // 设置调用用途。
                    .purpose("KNOWLEDGE_DOCUMENT_CHUNKS_EMBEDDING")
                    // 设置切片文本列表。
                    .texts(texts)
                    // 构建请求。
                    .build();
            // 调用真实或模拟 Embedding（嵌入）模型生成向量。
            EmbeddingModelResponse embeddingResponse = embeddingModelClient.embed(embeddingRequest);
            // 记录模型调用耗时。
            long embeddingLatencyMs = System.currentTimeMillis() - embeddingStart;
            // 校验模型返回数量和切片数量一致。
            validateEmbeddingResponse(chunks, embeddingResponse);
            // 记录模型调用成本。
            recordEmbeddingUsage(runId, embeddingRequest.getPurpose(), embeddingResponse, embeddingLatencyMs);
            // 构造向量写入条目。
            List<VectorStoreUpsertItem> items = java.util.stream.IntStream.range(0, chunks.size())
                    // 一一对应切片和向量。
                    .mapToObj(index -> VectorStoreUpsertItem.builder()
                            // 设置切片编号。
                            .chunkId(chunks.get(index).getChunkId())
                            // 设置向量。
                            .vector(embeddingResponse.getEmbeddings().get(index))
                            // 构建条目。
                            .build())
                    // 收集为列表。
                    .toList();
            // 构造向量替换请求。
            VectorStoreReplaceDocumentRequest replaceRequest = VectorStoreReplaceDocumentRequest.builder()
                    // 设置知识库编号。
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    // 设置文档编号。
                    .documentId(documentId)
                    // 设置供应商。
                    .provider(embeddingResponse.getProvider())
                    // 设置模型名称。
                    .modelName(embeddingResponse.getModelName())
                    // 设置向量维度。
                    .dimension(embeddingResponse.getDimension())
                    // 设置距离度量。
                    .distanceMetric("COSINE")
                    // 设置向量条目。
                    .items(items)
                    // 构建请求。
                    .build();
            // 短事务替换该文档全部旧向量并更新文档状态为 READY（可检索）。
            KnowledgeDocumentEntity updatedDocument = requiredEmbeddingPersistenceService().replaceEmbeddingsAndMarkReady(replaceRequest);
            // 返回向量化响应。
            return KnowledgeDocumentEmbeddingResponse.builder()
                    // 设置文档编号。
                    .documentId(documentId)
                    // 设置知识库编号。
                    .knowledgeBaseId(updatedDocument.getKnowledgeBaseId())
                    // 设置切片数量。
                    .chunkCount(chunks.size())
                    // 设置向量数量。
                    .embeddingCount(items.size())
                    // 设置供应商。
                    .provider(embeddingResponse.getProvider())
                    // 设置模型名称。
                    .modelName(embeddingResponse.getModelName())
                    // 设置维度。
                    .dimension(embeddingResponse.getDimension())
                    // 设置输入词元。
                    .promptTokens(embeddingResponse.getPromptTokens())
                    // 设置总词元。
                    .totalTokens(embeddingResponse.getTotalTokens())
                    // 设置文档状态。
                    .overallStatus(updatedDocument.getOverallStatus())
                    // 构建响应。
                    .build();
        } catch (RuntimeException exception) {
            // 向量化失败时标记文档向量化失败，便于查询和复盘。
            requiredKnowledgeDocumentStatusUseCase().markEmbeddingFailed(documentId, exception.getMessage());
            // 业务异常保持原样抛出。
            if (exception instanceof BusinessException businessException) {
                // 抛出原业务异常。
                throw businessException;
            }
            // 包装为知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文档向量化失败，documentId：" + documentId, exception);
        }
    }

    /**
     * searchByText（按文本检索）用于把查询文本向量化后执行 pgvector（PostgreSQL 向量扩展）TopK（前 K 个）检索。
     *
     * @param knowledgeBaseId 知识库编号；全库检索时可以为空
     * @param query 查询文本
     * @param topK 返回数量
     * @return 检索结果列表
     */
    public List<VectorStoreSearchResult> searchByText(String knowledgeBaseId, String query, Integer topK) {
        // 校验查询文本。
        if (query == null || query.isBlank()) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "查询文本不能为空");
        }
        // 生成模型调用运行编号，便于后续成本复盘。
        String runId = newId("rag_query_emb");
        // 记录模型调用开始时间。
        long embeddingStart = System.currentTimeMillis();
        // 构造查询嵌入请求。
        EmbeddingModelRequest embeddingRequest = EmbeddingModelRequest.builder()
                // 设置运行编号。
                .runId(runId)
                // 设置调用用途。
                .purpose("KNOWLEDGE_QUERY_EMBEDDING")
                // 设置单条查询文本。
                .texts(List.of(query))
                // 构建请求。
                .build();
        // 调用 Embedding（嵌入）模型生成查询向量。
        EmbeddingModelResponse embeddingResponse = embeddingModelClient.embed(embeddingRequest);
        // 记录模型调用耗时。
        long embeddingLatencyMs = System.currentTimeMillis() - embeddingStart;
        // 校验查询向量存在。
        if (embeddingResponse.getEmbeddings() == null || embeddingResponse.getEmbeddings().isEmpty()) {
            // 抛出模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "查询向量生成失败");
        }
        // 记录查询向量化成本。
        recordEmbeddingUsage(runId, embeddingRequest.getPurpose(), embeddingResponse, embeddingLatencyMs);
        // 执行向量检索。
        return requiredVectorStore().search(VectorStoreSearchRequest.builder()
                // 设置知识库编号。
                .knowledgeBaseId(knowledgeBaseId)
                // 如果知识库编号为空，则表示本次执行全库检索。
                .searchAllKnowledgeBases(knowledgeBaseId == null || knowledgeBaseId.isBlank())
                // 设置查询向量。
                .queryVector(embeddingResponse.getEmbeddings().get(0))
                // 设置 TopK（前 K 个）。
                .topK(topK)
                // 构建请求。
                .build());
    }

    /**
     * validateEmbeddingResponse（校验嵌入响应）用于确保模型返回和切片数量、维度一致。
     *
     * @param chunks 切片列表
     * @param response 嵌入模型响应
     */
    private void validateEmbeddingResponse(List<KnowledgeChunkEntity> chunks, EmbeddingModelResponse response) {
        // 校验响应和向量列表。
        if (response == null || response.getEmbeddings() == null || response.getEmbeddings().isEmpty()) {
            // 抛出模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "嵌入模型没有返回向量");
        }
        // 校验数量一致。
        if (response.getEmbeddings().size() != chunks.size()) {
            // 抛出模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "嵌入模型返回向量数量和切片数量不一致");
        }
        // 校验维度。
        if (response.getDimension() == null || response.getDimension() <= 0) {
            // 抛出模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "嵌入模型返回向量维度无效");
        }
    }

    /**
     * recordEmbeddingUsage（记录嵌入模型用量）用于保存 Embedding（嵌入）模型 Token（词元）和耗时。
     *
     * @param runId 运行编号
     * @param purpose 调用用途
     * @param response 嵌入模型响应
     * @param latencyMs 调用耗时
     */
    private void recordEmbeddingUsage(String runId, String purpose, EmbeddingModelResponse response, long latencyMs) {
        // 如果响应为空，说明模型调用没有成功返回，无需记录成功用量。
        if (response == null) {
            // 直接返回。
            return;
        }
        try {
            // 写入模型调用记录。
            modelUsageRecorder.record(ModelCallRecord.builder()
                    // 设置运行编号。
                    .runId(runId)
                    // 当前 RAG（检索增强生成）向量化阶段暂不绑定项目。
                    .projectId(null)
                    // 当前 RAG（检索增强生成）向量化阶段暂不绑定会话。
                    .conversationId(null)
                    // 设置供应商。
                    .provider(response.getProvider())
                    // 设置模型名称。
                    .modelName(response.getModelName())
                    // 设置调用用途。
                    .purpose(purpose)
                    // 设置输入 Token（词元）。
                    .promptTokens(response.getPromptTokens())
                    // Embedding（嵌入）没有生成文本，输出 Token（词元）记为 0。
                    .completionTokens(0)
                    // 设置总 Token（词元）。
                    .totalTokens(response.getTotalTokens())
                    // 设置调用耗时。
                    .latencyMs(latencyMs)
                    // 标记成功。
                    .success(true)
                    // 构建记录。
                    .build());
        } catch (RuntimeException exception) {
            // 模型用量记录失败不能中断 RAG（检索增强生成）向量入库主链路，后续可通过日志或观测系统补偿。
            log.warn("Embedding（嵌入）模型用量记录失败，runId：{}，purpose：{}，原因：{}", runId, purpose, exception.getMessage());
        }
    }

    /**
     * requiredDocument（获取必需文档）用于查询并校验文档存在。
     *
     * @param documentId 知识文档编号
     * @return 知识文档实体
     */
    private KnowledgeDocumentEntity requiredDocument(String documentId) {
        // 查询文档实体。
        return requiredKnowledgeDocumentRepository().findByDocumentId(documentId)
                // 不存在时抛出明确错误。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
    }

    /**
     * requiredKnowledgeDocumentRepository（获取必需知识文档仓储）用于处理未启用数据库的场景。
     *
     * @return 知识文档仓储
     */
    private KnowledgeDocumentRepository requiredKnowledgeDocumentRepository() {
        // 从 Spring（Spring 框架）容器中获取知识文档仓储。
        KnowledgeDocumentRepository repository = knowledgeDocumentRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档向量化需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
    }

    /**
     * requiredKnowledgeDocumentStatusUseCase（获取必需知识文档状态用例）用于处理未启用数据库的场景。
     *
     * @return 知识文档状态用例
     */
    private KnowledgeDocumentStatusUseCase requiredKnowledgeDocumentStatusUseCase() {
        // 从 Spring（Spring 框架）容器中获取状态用例。
        KnowledgeDocumentStatusUseCase useCase = knowledgeDocumentStatusUseCaseProvider.getIfAvailable();
        // 如果状态用例不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (useCase == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档状态更新需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用状态用例。
        return useCase;
    }

    /**
     * requiredKnowledgeChunkRepository（获取必需知识切片仓储）用于处理未启用数据库的场景。
     *
     * @return 知识切片仓储
     */
    private KnowledgeChunkRepository requiredKnowledgeChunkRepository() {
        // 从 Spring（Spring 框架）容器中获取知识切片仓储。
        KnowledgeChunkRepository repository = knowledgeChunkRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识切片向量化需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
    }

    /**
     * requiredVectorStore（获取必需向量存储）用于处理未启用数据库的场景。
     *
     * @return 向量存储
     */
    private VectorStore requiredVectorStore() {
        // 从 Spring（Spring 框架）容器中获取向量存储。
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        // 如果向量存储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (vectorStore == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "向量存储需要启用 db Profile（数据库配置环境）");
        }
        // 返回向量存储。
        return vectorStore;
    }

    /**
     * requiredEmbeddingPersistenceService（获取必需向量化持久化服务）用于处理未启用数据库的场景。
     *
     * @return 向量化持久化服务
     */
    private KnowledgeDocumentEmbeddingPersistenceService requiredEmbeddingPersistenceService() {
        // 从 Spring（Spring 框架）容器中获取向量化持久化服务。
        KnowledgeDocumentEmbeddingPersistenceService service = embeddingPersistenceServiceProvider.getIfAvailable();
        // 如果服务不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (service == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档向量化持久化需要启用 db Profile（数据库配置环境）");
        }
        // 返回服务。
        return service;
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成本地开发阶段足够唯一的编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * KnowledgeDocumentEmbeddingResponse（知识文档向量化响应）。
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeDocumentEmbeddingResponse {

        /** documentId（知识文档编号）表示完成向量化的文档。 */
        private String documentId;

        /** knowledgeBaseId（知识库编号）表示文档所属知识库。 */
        private String knowledgeBaseId;

        /** chunkCount（切片数量）表示参与向量化的切片数量。 */
        private Integer chunkCount;

        /** embeddingCount（向量数量）表示写入向量表的向量数量。 */
        private Integer embeddingCount;

        /** provider（供应商）表示嵌入模型供应商。 */
        private String provider;

        /** modelName（模型名称）表示嵌入模型名称。 */
        private String modelName;

        /** dimension（向量维度）表示向量维度。 */
        private Integer dimension;

        /** promptTokens（输入词元）表示向量化文本消耗。 */
        private Integer promptTokens;

        /** totalTokens（总词元）表示总消耗。 */
        private Integer totalTokens;

        /** overallStatus（整体状态）表示文档处理状态。 */
        private String overallStatus;
    }
}

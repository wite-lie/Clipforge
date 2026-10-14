package com.clipforge.knowledge.application.processing;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import java.util.List;

/**
 * KnowledgeDocumentChunkingUseCase（知识文档解析切片用例）。
 *
 * <p>该端口只描述 Knowledge 领域的解析、切片、元数据同步和文档状态更新，
 * 不暴露 API DTO，也不依赖 Redis、RocketMQ 或 legacy 应用服务。
 */
public interface KnowledgeDocumentChunkingUseCase {

    /**
     * processDocument（处理文档）用于执行一次完整的解析和切片。
     *
     * @param documentId 知识文档编号
     * @return Knowledge 内部切片结果
     */
    KnowledgeDocumentChunkingResult processDocument(String documentId);

    /**
     * listChunks（查询切片）用于读取指定文档下的有效切片。
     *
     * @param documentId 知识文档编号
     * @return 有效切片列表
     */
    List<KnowledgeChunkEntity> listChunks(String documentId);
}

package com.clipforge.rag.process;

import java.util.Optional;

/**
 * KnowledgeDocumentProcessStatusCache（知识文档处理状态缓存）。
 *
 * <p>该接口用于屏蔽 Redis（缓存）实现细节，应用层后续只依赖该抽象读取和刷新异步处理状态。
 */
public interface KnowledgeDocumentProcessStatusCache {

    /**
     * save（保存）用于写入或刷新知识文档处理状态。
     *
     * @param status 处理状态
     */
    void save(KnowledgeDocumentProcessStatus status);

    /**
     * findByDocumentId（按文档编号查询）用于读取最近一次处理状态。
     *
     * @param documentId 知识文档编号
     * @return 可能存在的处理状态
     */
    Optional<KnowledgeDocumentProcessStatus> findByDocumentId(String documentId);

    /**
     * findByProcessLogId（按处理日志编号查询）用于读取指定处理日志对应的状态。
     *
     * @param processLogId 处理日志编号
     * @return 可能存在的处理状态
     */
    Optional<KnowledgeDocumentProcessStatus> findByProcessLogId(String processLogId);

    /**
     * deleteByDocumentId（按文档编号删除）用于清理过期或无效的处理状态缓存。
     *
     * @param documentId 知识文档编号
     */
    void deleteByDocumentId(String documentId);
}

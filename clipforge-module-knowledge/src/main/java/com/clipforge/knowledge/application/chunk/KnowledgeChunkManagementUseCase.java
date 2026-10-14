package com.clipforge.knowledge.application.chunk;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import java.util.Optional;

/**
 * KnowledgeChunkManagementUseCase（知识切片管理用例）。
 *
 * <p>该端口负责人工切片的查询、新增、更新、启停和逻辑删除，
 * 不依赖 API 请求 DTO，也不负责 HTTP 响应转换。
 */
public interface KnowledgeChunkManagementUseCase {

    /**
     * findChunk（查询切片）用于读取指定文档下的切片。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 可能存在的切片
     */
    Optional<KnowledgeChunkEntity> findChunk(String documentId, String chunkId);

    /**
     * createChunk（新增切片）用于人工补充一个知识切片。
     *
     * @param documentId 知识文档编号
     * @param command 新增切片命令
     * @return 已保存切片
     */
    KnowledgeChunkEntity createChunk(String documentId, CreateChunkCommand command);

    /**
     * updateChunk（更新切片）用于人工修改切片正文或元数据。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param command 更新切片命令
     * @return 更新后切片
     */
    KnowledgeChunkEntity updateChunk(String documentId, String chunkId, UpdateChunkCommand command);

    /**
     * setEnabled（设置切片启用状态）用于控制切片是否参与检索。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param enabled 是否启用
     * @return 更新后切片
     */
    KnowledgeChunkEntity setEnabled(String documentId, String chunkId, boolean enabled);

    /**
     * deleteChunk（删除切片）用于逻辑删除一个知识切片并标记文档待重新索引。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 被删除切片编号
     */
    String deleteChunk(String documentId, String chunkId);

    /**
     * CreateChunkCommand（新增切片命令）是 API DTO 与 Knowledge 内部实体之间的隔离对象。
     *
     * @param content 切片正文
     * @param titlePath 标题路径
     * @param metadataJson 元数据 JSON
     * @param chunkIndex 可选切片序号
     * @param enabled 可选启用状态
     */
    record CreateChunkCommand(
            String content,
            String titlePath,
            String metadataJson,
            Integer chunkIndex,
            Boolean enabled) {
    }

    /**
     * UpdateChunkCommand（更新切片命令）用于承载允许修改的字段。
     *
     * @param titlePath 标题路径，可为空表示不修改
     * @param content 正文，可为空表示不修改
     * @param metadataJson 元数据 JSON，可为空表示不修改
     */
    record UpdateChunkCommand(
            String titlePath,
            String content,
            String metadataJson) {
    }
}

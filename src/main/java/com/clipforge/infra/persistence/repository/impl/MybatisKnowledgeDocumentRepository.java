package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentOverallStatus;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentSourceType;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentStepStatus;
import com.clipforge.infra.persistence.mapper.KnowledgeDocumentMapper;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisKnowledgeDocumentRepository（MyBatis 知识文档仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问 cf_knowledge_document（知识文档表）。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisKnowledgeDocumentRepository implements KnowledgeDocumentRepository {

    /** knowledgeDocumentMapper（知识文档映射器）负责执行知识文档表 CRUD（增删改查）。 */
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    /**
     * save（保存）用于写入知识文档实体。
     *
     * @param entity 知识文档实体
     * @return 已保存知识文档实体
     */
    @Override
    public KnowledgeDocumentEntity save(KnowledgeDocumentEntity entity) {
        // 插入知识文档实体。
        knowledgeDocumentMapper.insert(entity);
        // 返回已保存实体。
        return entity;
    }

    /**
     * findByDocumentId（按知识文档编号查询）用于读取文档详情。
     *
     * @param documentId 知识文档编号
     * @return 可能存在的知识文档实体
     */
    @Override
    public Optional<KnowledgeDocumentEntity> findByDocumentId(String documentId) {
        // 创建按文档编号查询且排除逻辑删除数据的条件。
        LambdaQueryWrapper<KnowledgeDocumentEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(knowledgeDocumentMapper.selectOne(queryWrapper));
    }

    /**
     * listByKnowledgeBaseId（按知识库编号查询列表）用于读取某个知识库下的文档。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 知识文档实体列表
     */
    @Override
    public List<KnowledgeDocumentEntity> listByKnowledgeBaseId(String knowledgeBaseId) {
        // 创建按知识库编号查询、排除逻辑删除并按创建时间倒序排列的条件。
        LambdaQueryWrapper<KnowledgeDocumentEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                // knowledgeBaseId（知识库编号）必须匹配。
                .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                // 按创建时间倒序排列，方便优先看到最近上传的文档。
                .orderByDesc(KnowledgeDocumentEntity::getCreateTime);
        // 执行列表查询并返回。
        return knowledgeDocumentMapper.selectList(queryWrapper);
    }

    /**
     * markProcessingIfNotProcessing（非处理中时标记处理中）用于异步任务提交前抢占文档处理权。
     *
     * @param documentId 知识文档编号
     * @return true 表示抢占成功，false 表示文档不存在或已经处于 PROCESSING（处理中）
     */
    @Override
    public boolean markProcessingIfNotProcessing(String documentId) {
        // 获取当前时间。
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 创建轻量 CAS（比较并交换）更新条件：只有文档不是处理中时才允许改为处理中。
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                // 设置解析状态为处理中。
                .set(KnowledgeDocumentEntity::getParseStatus, KnowledgeDocumentStepStatus.PROCESSING.value())
                // 切片阶段等待解析完成。
                .set(KnowledgeDocumentEntity::getChunkStatus, KnowledgeDocumentStepStatus.PENDING.value())
                // 向量化阶段等待切片完成。
                .set(KnowledgeDocumentEntity::getEmbeddingStatus, KnowledgeDocumentStepStatus.PENDING.value())
                // 整体状态标记为处理中。
                .set(KnowledgeDocumentEntity::getOverallStatus, KnowledgeDocumentOverallStatus.PROCESSING.value())
                // 清空上一次错误消息。
                .set(KnowledgeDocumentEntity::getErrorMessage, null)
                // 更新最近处理时间。
                .set(KnowledgeDocumentEntity::getLastProcessTime, now)
                // 更新时间。
                .set(KnowledgeDocumentEntity::getUpdateTime, now)
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeDocumentEntity::getDeleted, 0)
                // 当前整体状态不能已经是 PROCESSING（处理中）。
                .ne(KnowledgeDocumentEntity::getOverallStatus, KnowledgeDocumentOverallStatus.PROCESSING.value());
        // 执行更新，并根据影响行数判断是否抢占成功。
        return knowledgeDocumentMapper.update(null, updateWrapper) > 0;
    }

    /**
     * markChunked（标记已切片）用于在文档解析和切片成功后更新文档状态。
     *
     * @param documentId 知识文档编号
     * @param chunkCount 切片数量
     * @param charCount 字符数
     * @param tokenCount 估算 Token（词元）数量
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markChunked(String documentId, int chunkCount, int charCount, int tokenCount) {
        // 获取当前时间。
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 创建文档状态更新条件。
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                // 设置解析状态为成功。
                .set(KnowledgeDocumentEntity::getParseStatus, KnowledgeDocumentStepStatus.SUCCESS.value())
                // 设置切片状态为成功。
                .set(KnowledgeDocumentEntity::getChunkStatus, KnowledgeDocumentStepStatus.SUCCESS.value())
                // 向量化状态仍为待处理。
                .set(KnowledgeDocumentEntity::getEmbeddingStatus, KnowledgeDocumentStepStatus.PENDING.value())
                // 整体状态进入 CHUNKED（已切片）中间状态。
                .set(KnowledgeDocumentEntity::getOverallStatus, KnowledgeDocumentOverallStatus.CHUNKED.value())
                // 清空错误消息。
                .set(KnowledgeDocumentEntity::getErrorMessage, null)
                // 设置切片数量。
                .set(KnowledgeDocumentEntity::getChunkCount, chunkCount)
                // 设置字符数。
                .set(KnowledgeDocumentEntity::getCharCount, charCount)
                // 设置估算 Token（词元）数量。
                .set(KnowledgeDocumentEntity::getTokenCount, tokenCount)
                // 设置最近处理时间。
                .set(KnowledgeDocumentEntity::getLastProcessTime, now)
                // 设置更新时间。
                .set(KnowledgeDocumentEntity::getUpdateTime, now)
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 执行状态更新。
        knowledgeDocumentMapper.update(null, updateWrapper);
        // 返回更新后的文档实体。
        return findByDocumentId(documentId).orElseThrow();
    }

    /**
     * markEmbedded（标记已向量化）用于在文档向量写入成功后把文档置为 READY（可检索）。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markEmbedded(String documentId) {
        // 获取当前时间。
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 创建文档向量化成功更新条件。
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                // 保持解析状态成功。
                .set(KnowledgeDocumentEntity::getParseStatus, KnowledgeDocumentStepStatus.SUCCESS.value())
                // 保持切片状态成功。
                .set(KnowledgeDocumentEntity::getChunkStatus, KnowledgeDocumentStepStatus.SUCCESS.value())
                // 设置向量化状态成功。
                .set(KnowledgeDocumentEntity::getEmbeddingStatus, KnowledgeDocumentStepStatus.SUCCESS.value())
                // 整体状态进入 READY（可检索）。
                .set(KnowledgeDocumentEntity::getOverallStatus, KnowledgeDocumentOverallStatus.READY.value())
                // 清空错误消息。
                .set(KnowledgeDocumentEntity::getErrorMessage, null)
                // 设置最近处理时间。
                .set(KnowledgeDocumentEntity::getLastProcessTime, now)
                // 设置更新时间。
                .set(KnowledgeDocumentEntity::getUpdateTime, now)
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 执行状态更新。
        knowledgeDocumentMapper.update(null, updateWrapper);
        // 返回更新后的文档实体。
        return findByDocumentId(documentId).orElseThrow();
    }

    /**
     * markEmbeddingFailed（标记向量化失败）用于在 Embedding（嵌入）模型调用或向量入库失败时更新文档状态。
     *
     * @param documentId 知识文档编号
     * @param errorMessage 错误消息
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markEmbeddingFailed(String documentId, String errorMessage) {
        // 创建文档向量化失败更新条件。
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                // 设置向量化状态失败。
                .set(KnowledgeDocumentEntity::getEmbeddingStatus, KnowledgeDocumentStepStatus.FAILED.value())
                // 整体状态为失败。
                .set(KnowledgeDocumentEntity::getOverallStatus, KnowledgeDocumentOverallStatus.FAILED.value())
                // 写入错误消息。
                .set(KnowledgeDocumentEntity::getErrorMessage, shorten(errorMessage))
                // 设置更新时间。
                .set(KnowledgeDocumentEntity::getUpdateTime, java.time.LocalDateTime.now())
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 执行状态更新。
        knowledgeDocumentMapper.update(null, updateWrapper);
        // 返回更新后的文档实体。
        return findByDocumentId(documentId).orElseThrow();
    }

    /**
     * markReindexRequired（标记待重新索引）用于切片被人工修改后让文档退出可检索状态。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markReindexRequired(String documentId) {
        // 获取当前时间。
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 切片被人工改动后，保留解析和切片成功状态，仅把向量化状态和整体状态切回待重建。
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                .set(KnowledgeDocumentEntity::getParseStatus, KnowledgeDocumentStepStatus.SUCCESS.value())
                .set(KnowledgeDocumentEntity::getChunkStatus, KnowledgeDocumentStepStatus.SUCCESS.value())
                .set(KnowledgeDocumentEntity::getEmbeddingStatus, KnowledgeDocumentStepStatus.PENDING.value())
                .set(KnowledgeDocumentEntity::getOverallStatus, KnowledgeDocumentOverallStatus.PENDING_REINDEX.value())
                .set(KnowledgeDocumentEntity::getErrorMessage, null)
                .set(KnowledgeDocumentEntity::getLastProcessTime, now)
                .set(KnowledgeDocumentEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 执行更新。
        knowledgeDocumentMapper.update(null, updateWrapper);
        // 返回最新实体。
        return findByDocumentId(documentId).orElseThrow();
    }

    /**
     * markEnabled（标记启用或禁用）用于切换文档是否参与检索。
     *
     * @param documentId 知识文档编号
     * @param enabled 是否启用，1 表示启用，0 表示禁用
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markEnabled(String documentId, Integer enabled) {
        // 获取当前时间。
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 构造启用状态更新条件。
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                .set(KnowledgeDocumentEntity::getEnabled, enabled)
                .set(KnowledgeDocumentEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 执行更新。
        knowledgeDocumentMapper.update(null, updateWrapper);
        // 返回最新实体。
        return findByDocumentId(documentId).orElseThrow();
    }

    /**
     * markDeleted（标记删除）用于将文档逻辑删除。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markDeleted(String documentId) {
        // 获取当前时间。
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 构造逻辑删除条件。
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                .set(KnowledgeDocumentEntity::getDeleted, 1)
                .set(KnowledgeDocumentEntity::getEnabled, 0)
                .set(KnowledgeDocumentEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 执行删除。
        knowledgeDocumentMapper.update(null, updateWrapper);
        // 返回最新实体。
        return findByDocumentId(documentId).orElseThrow();
    }

    /**
     * markFailed（标记失败）用于在文档解析或切片失败后更新文档状态。
     *
     * @param documentId 知识文档编号
     * @param errorMessage 错误消息
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markFailed(String documentId, String errorMessage) {
        // 创建文档失败状态更新条件。
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                // 设置解析状态为失败。
                .set(KnowledgeDocumentEntity::getParseStatus, KnowledgeDocumentStepStatus.FAILED.value())
                // 设置切片状态为失败。
                .set(KnowledgeDocumentEntity::getChunkStatus, KnowledgeDocumentStepStatus.FAILED.value())
                // 整体状态为失败。
                .set(KnowledgeDocumentEntity::getOverallStatus, KnowledgeDocumentOverallStatus.FAILED.value())
                // 写入错误消息。
                .set(KnowledgeDocumentEntity::getErrorMessage, shorten(errorMessage))
                // 设置更新时间。
                .set(KnowledgeDocumentEntity::getUpdateTime, java.time.LocalDateTime.now())
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 执行状态更新。
        knowledgeDocumentMapper.update(null, updateWrapper);
        // 返回更新后的文档实体。
        return findByDocumentId(documentId).orElseThrow();
    }

    /**
     * shorten（缩短）用于避免过长错误消息写入数据库。
     *
     * @param value 原始错误消息
     * @return 缩短后的错误消息
     */
    @Override
    public KnowledgeDocumentEntity refreshRemoteSourceDocument(String documentId, String sourceLocation, String originalFilename, String storedFilename, String filePath, String contentType, Long fileSize, String fileHash, String lastEtag, String lastModified, String lastContentHash) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                .set(KnowledgeDocumentEntity::getSourceType, KnowledgeDocumentSourceType.URL.value())
                .set(KnowledgeDocumentEntity::getSourceLocation, sourceLocation)
                .set(KnowledgeDocumentEntity::getOriginalFilename, originalFilename)
                .set(KnowledgeDocumentEntity::getStoredFilename, storedFilename)
                .set(KnowledgeDocumentEntity::getFilePath, filePath)
                .set(KnowledgeDocumentEntity::getContentType, contentType)
                .set(KnowledgeDocumentEntity::getFileSize, fileSize)
                .set(KnowledgeDocumentEntity::getFileHash, fileHash)
                .set(KnowledgeDocumentEntity::getParseStatus, KnowledgeDocumentStepStatus.PENDING.value())
                .set(KnowledgeDocumentEntity::getChunkStatus, KnowledgeDocumentStepStatus.PENDING.value())
                .set(KnowledgeDocumentEntity::getEmbeddingStatus, KnowledgeDocumentStepStatus.PENDING.value())
                .set(KnowledgeDocumentEntity::getOverallStatus, KnowledgeDocumentOverallStatus.PENDING.value())
                .set(KnowledgeDocumentEntity::getErrorMessage, null)
                .set(KnowledgeDocumentEntity::getChunkCount, 0)
                .set(KnowledgeDocumentEntity::getCharCount, 0)
                .set(KnowledgeDocumentEntity::getTokenCount, 0)
                .set(KnowledgeDocumentEntity::getLastEtag, lastEtag)
                .set(KnowledgeDocumentEntity::getLastModified, lastModified)
                .set(KnowledgeDocumentEntity::getLastContentHash, lastContentHash)
                .set(KnowledgeDocumentEntity::getLastProcessTime, null)
                .set(KnowledgeDocumentEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        knowledgeDocumentMapper.update(null, updateWrapper);
        return findByDocumentId(documentId).orElseThrow();
    }

    /**
     * updateScheduleConfig锛堟洿鏂板畾鏃跺悓姝ラ厤缃級鐢ㄤ簬鍚屾鏂囨。琛ㄩ噷鐨勫畾鏃跺瓧娈点€?
     *
     * @param documentId 鐭ヨ瘑鏂囨。缂栧彿
     * @param scheduleEnabled 鏄惁鍚敤瀹氭椂鍚屾
     * @param scheduleCron Cron锛堝畾鏃惰〃杈惧紡锛?
     * @return 鏇存柊鍚庣殑鐭ヨ瘑鏂囨。瀹炰綋
     */
    @Override
    public KnowledgeDocumentEntity updateScheduleConfig(String documentId, Integer scheduleEnabled, String scheduleCron) {
        // 鑾峰彇褰撳墠鏃堕棿銆?
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 鍒涘缓瀹氭椂鍚屾閰嶇疆鏇存柊鏉′欢銆?
        LambdaUpdateWrapper<KnowledgeDocumentEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentEntity>()
                // 璁剧疆鏄惁鍚敤瀹氭椂鍚屾銆?
                .set(KnowledgeDocumentEntity::getScheduleEnabled, scheduleEnabled)
                // 璁剧疆 Cron锛堝畾鏃惰〃杈惧紡锛夈?
                .set(KnowledgeDocumentEntity::getScheduleCron, scheduleCron)
                // 璁剧疆鏇存柊鏃堕棿銆?
                .set(KnowledgeDocumentEntity::getUpdateTime, now)
                // documentId锛堢煡璇嗘枃妗ｇ紪鍙凤級蹇呴』鍖归厤銆?
                .eq(KnowledgeDocumentEntity::getDocumentId, documentId)
                // deleted锛堥€昏緫鍒犻櫎锛夊繀椤讳负姝ｅ父銆?
                .eq(KnowledgeDocumentEntity::getDeleted, 0);
        // 鎵ц鏇存柊銆?
        knowledgeDocumentMapper.update(null, updateWrapper);
        // 杩斿洖鏂板疄浣撱€?
        return findByDocumentId(documentId).orElseThrow();
    }

    private String shorten(String value) {
        // 如果错误消息为空，则返回默认文本。
        if (value == null || value.isBlank()) {
            // 返回默认失败原因。
            return "文档处理失败";
        }
        // 如果长度不超过 1000，则直接返回。
        if (value.length() <= 1000) {
            // 返回原始文本。
            return value;
        }
        // 返回前 1000 个字符。
        return value.substring(0, 1000);
    }
}

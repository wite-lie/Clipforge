package com.clipforge.application;

import com.clipforge.api.dto.CreateKnowledgeChunkRequest;
import com.clipforge.api.dto.KnowledgeChunkResponse;
import com.clipforge.api.dto.KnowledgeDocumentProcessLogResponse;
import com.clipforge.api.dto.KnowledgeDocumentResponse;
import com.clipforge.api.dto.UpdateKnowledgeChunkRequest;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentOverallStatus;
import com.clipforge.infra.persistence.repository.KnowledgeChunkRepository;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentProcessLogRepository;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeDocumentManagementApplicationService（知识文档管理应用服务）。
 *
 * <p>该服务负责文档与切片的管理接口：处理日志查询、切片新增/更新/启停/删除，以及文档启停/删除。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentManagementApplicationService {

    /** knowledgeDocumentApplicationService（知识文档应用服务）用于复用文档详情响应转换。 */
    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于读取和更新文档状态。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** knowledgeChunkRepositoryProvider（知识切片仓储提供器）用于读取和更新切片状态。 */
    private final ObjectProvider<KnowledgeChunkRepository> knowledgeChunkRepositoryProvider;

    /** processLogRepositoryProvider（处理日志仓储提供器）用于查询文档处理历史。 */
    private final ObjectProvider<KnowledgeDocumentProcessLogRepository> processLogRepositoryProvider;

    /**
     * listProcessLogs（查询处理日志）用于读取某个文档的全部处理历史。
     *
     * @param documentId 知识文档编号
     * @return 处理日志列表
     */
    public List<KnowledgeDocumentProcessLogResponse> listProcessLogs(String documentId) {
        requiredDocument(documentId);
        return requiredProcessLogRepository().listByDocumentId(documentId).stream()
                .map(this::toProcessLogResponse)
                .toList();
    }

    /**
     * queryChunk（查询单个切片）用于读取某个文档下的单个切片详情。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 知识切片响应
     */
    public KnowledgeChunkResponse queryChunk(String documentId, String chunkId) {
        requiredDocument(documentId);
        KnowledgeChunkEntity chunk = requiredChunk(chunkId);
        ensureChunkBelongsToDocument(documentId, chunk);
        return toChunkResponse(chunk);
    }

    /**
     * createChunk（新增切片）用于人工补充一个切片。
     *
     * @param documentId 知识文档编号
     * @param request 新增切片请求
     * @return 新增后的切片响应
     */
    @Transactional
    public KnowledgeChunkResponse createChunk(String documentId, CreateKnowledgeChunkRequest request) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        assertDocumentNotProcessing(document);
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新增切片内容不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        List<KnowledgeChunkEntity> existingChunks = requiredKnowledgeChunkRepository().listByDocumentId(documentId);
        Integer chunkIndex = request.getChunkIndex();
        if (chunkIndex == null) {
            chunkIndex = existingChunks.stream()
                    .map(KnowledgeChunkEntity::getChunkIndex)
                    .filter(java.util.Objects::nonNull)
                    .max(Integer::compareTo)
                    .map(value -> value + 1)
                    .orElse(0);
        }
        String content = request.getContent().strip();
        KnowledgeChunkEntity chunk = KnowledgeChunkEntity.builder()
                .id(newId("knowledge_chunk_pk"))
                .chunkId(newId("knowledge_chunk"))
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .documentId(documentId)
                .chunkIndex(chunkIndex)
                .titlePath(blankToNull(request.getTitlePath()))
                .content(content)
                .contentHash(sha256(content))
                .charCount(content.length())
                .tokenCount(estimateTokenCount(content))
                .metadataJson(blankToNull(request.getMetadataJson()))
                .enabled(normalizeEnabled(request.getEnabled()))
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        KnowledgeChunkEntity savedChunk = requiredKnowledgeChunkRepository().saveChunk(chunk);
        requiredKnowledgeDocumentRepository().markReindexRequired(documentId);
        return toChunkResponse(savedChunk);
    }

    /**
     * updateChunk（更新切片）用于修改人工维护的切片内容。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param request 更新切片请求
     * @return 更新后的切片响应
     */
    @Transactional
    public KnowledgeChunkResponse updateChunk(String documentId, String chunkId, UpdateKnowledgeChunkRequest request) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        assertDocumentNotProcessing(document);
        KnowledgeChunkEntity chunk = requiredChunk(chunkId);
        ensureChunkBelongsToDocument(documentId, chunk);
        if (request == null || (request.getTitlePath() == null && request.getContent() == null && request.getMetadataJson() == null)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "更新切片至少需要传入一个字段");
        }
        if (request.getTitlePath() != null) {
            chunk.setTitlePath(blankToNull(request.getTitlePath()));
        }
        if (request.getContent() != null) {
            if (request.getContent().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "切片内容不能为空");
            }
            String content = request.getContent().strip();
            chunk.setContent(content);
            chunk.setContentHash(sha256(content));
            chunk.setCharCount(content.length());
            chunk.setTokenCount(estimateTokenCount(content));
        }
        if (request.getMetadataJson() != null) {
            chunk.setMetadataJson(blankToNull(request.getMetadataJson()));
        }
        KnowledgeChunkEntity savedChunk = requiredKnowledgeChunkRepository().updateChunk(chunk);
        requiredKnowledgeDocumentRepository().markReindexRequired(documentId);
        return toChunkResponse(savedChunk);
    }

    /**
     * setChunkEnabled（切片启停）用于临时让某个切片退出或重新参与检索。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param enabled 是否启用
     * @return 更新后的切片响应
     */
    @Transactional
    public KnowledgeChunkResponse setChunkEnabled(String documentId, String chunkId, boolean enabled) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        assertDocumentNotProcessing(document);
        KnowledgeChunkEntity chunk = requiredChunk(chunkId);
        ensureChunkBelongsToDocument(documentId, chunk);
        KnowledgeChunkEntity savedChunk = requiredKnowledgeChunkRepository().markEnabled(chunkId, enabled ? 1 : 0);
        return toChunkResponse(savedChunk);
    }

    /**
     * deleteChunk（删除切片）用于逻辑删除单个切片。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 被删除的切片编号
     */
    @Transactional
    public String deleteChunk(String documentId, String chunkId) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        assertDocumentNotProcessing(document);
        KnowledgeChunkEntity chunk = requiredChunk(chunkId);
        ensureChunkBelongsToDocument(documentId, chunk);
        requiredKnowledgeChunkRepository().markDeleted(chunkId);
        requiredKnowledgeDocumentRepository().markReindexRequired(documentId);
        return chunkId;
    }

    /**
     * setDocumentEnabled（文档启停）用于让文档临时下线或恢复。
     *
     * @param documentId 知识文档编号
     * @param enabled 是否启用
     * @return 文档响应
     */
    @Transactional
    public KnowledgeDocumentResponse setDocumentEnabled(String documentId, boolean enabled) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        assertDocumentNotProcessing(document);
        requiredKnowledgeDocumentRepository().markEnabled(documentId, enabled ? 1 : 0);
        return knowledgeDocumentApplicationService.queryDocument(documentId);
    }

    /**
     * deleteDocument（删除文档）用于逻辑删除文档与其全部切片。
     *
     * @param documentId 知识文档编号
     * @return 被删除的文档编号
     */
    @Transactional
    public String deleteDocument(String documentId) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        assertDocumentNotProcessing(document);
        requiredKnowledgeChunkRepository().markDeletedByDocumentId(documentId);
        requiredKnowledgeDocumentRepository().markDeleted(documentId);
        return documentId;
    }

    /**
     * toProcessLogResponse（转换处理日志响应）用于对外返回可读的历史记录。
     *
     * @param entity 处理日志实体
     * @return 处理日志响应
     */
    private KnowledgeDocumentProcessLogResponse toProcessLogResponse(KnowledgeDocumentProcessLogEntity entity) {
        return KnowledgeDocumentProcessLogResponse.builder()
                .processLogId(entity.getProcessLogId())
                .documentId(entity.getDocumentId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .triggerType(entity.getTriggerType())
                .status(entity.getStatus())
                .phase(entity.getPhase())
                .parseDurationMs(entity.getParseDurationMs())
                .chunkDurationMs(entity.getChunkDurationMs())
                .embeddingDurationMs(entity.getEmbeddingDurationMs())
                .persistDurationMs(entity.getPersistDurationMs())
                .totalDurationMs(entity.getTotalDurationMs())
                .chunkCount(entity.getChunkCount())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * toChunkResponse（转换切片响应）用于返回单个切片详情。
     *
     * @param entity 切片实体
     * @return 切片响应
     */
    private KnowledgeChunkResponse toChunkResponse(KnowledgeChunkEntity entity) {
        return KnowledgeChunkResponse.builder()
                .chunkId(entity.getChunkId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .documentId(entity.getDocumentId())
                .chunkIndex(entity.getChunkIndex())
                .titlePath(entity.getTitlePath())
                .content(entity.getContent())
                .contentHash(entity.getContentHash())
                .charCount(entity.getCharCount())
                .tokenCount(entity.getTokenCount())
                .metadataJson(entity.getMetadataJson())
                .enabled(entity.getEnabled())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    /**
     * requiredDocument（获取必需文档）用于校验文档存在。
     *
     * @param documentId 知识文档编号
     * @return 文档实体
     */
    private KnowledgeDocumentEntity requiredDocument(String documentId) {
        return requiredKnowledgeDocumentRepository().findByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
    }

    /**
     * requiredChunk（获取必需切片）用于校验切片存在。
     *
     * @param chunkId 知识切片编号
     * @return 切片实体
     */
    private KnowledgeChunkEntity requiredChunk(String chunkId) {
        return requiredKnowledgeChunkRepository().findByChunkId(chunkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识切片，chunkId：" + chunkId));
    }

    /**
     * ensureChunkBelongsToDocument（校验切片归属）用于防止跨文档误操作。
     *
     * @param documentId 文档编号
     * @param chunk 切片实体
     */
    private void ensureChunkBelongsToDocument(String documentId, KnowledgeChunkEntity chunk) {
        if (!documentId.equals(chunk.getDocumentId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "切片不属于当前文档，chunkId：" + chunk.getChunkId());
        }
    }

    /**
     * assertDocumentNotProcessing（校验文档未处理中）用于避免与自动链路冲突。
     *
     * @param document 文档实体
     */
    private void assertDocumentNotProcessing(KnowledgeDocumentEntity document) {
        if (KnowledgeDocumentOverallStatus.PROCESSING.value().equals(document.getOverallStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档正在处理中，暂不允许执行管理操作");
        }
    }

    /**
     * requiredKnowledgeDocumentRepository（获取必需文档仓储）。
     *
     * @return 文档仓储
     */
    private KnowledgeDocumentRepository requiredKnowledgeDocumentRepository() {
        KnowledgeDocumentRepository repository = knowledgeDocumentRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "文档管理接口需要启用 db Profile");
        }
        return repository;
    }

    /**
     * requiredKnowledgeChunkRepository（获取必需切片仓储）。
     *
     * @return 切片仓储
     */
    private KnowledgeChunkRepository requiredKnowledgeChunkRepository() {
        KnowledgeChunkRepository repository = knowledgeChunkRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "切片管理接口需要启用 db Profile");
        }
        return repository;
    }

    /**
     * requiredProcessLogRepository（获取必需处理日志仓储）。
     *
     * @return 处理日志仓储
     */
    private KnowledgeDocumentProcessLogRepository requiredProcessLogRepository() {
        KnowledgeDocumentProcessLogRepository repository = processLogRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "处理日志查询需要启用 db Profile");
        }
        return repository;
    }

    /**
     * normalizeEnabled（标准化启用值）用于把布尔值转成数据库整数。
     *
     * @param enabled 是否启用
     * @return 1 或 0
     */
    private Integer normalizeEnabled(Boolean enabled) {
        return enabled == null || enabled ? 1 : 0;
    }

    /**
     * blankToNull（空白转空）用于避免数据库里存入无意义空白字符串。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    /**
     * estimateTokenCount（估算 Token 数）用于管理接口简单统计。
     *
     * @param text 文本
     * @return 估算 Token 数
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 2.0));
    }

    /**
     * sha256（计算 SHA-256）用于人工新增或更新切片时生成内容哈希。
     *
     * @param content 正文
     * @return 哈希字符串
     */
    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256 哈希算法不可用", exception);
        }
    }

    /**
     * newId（生成编号）用于创建带前缀的唯一编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

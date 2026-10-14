package com.clipforge.knowledge.application.chunk;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.application.document.KnowledgeDocumentManagementUseCase;
import com.clipforge.knowledge.application.processing.KnowledgeDocumentStatusUseCase;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentOverallStatus;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeChunkRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeChunkManagementApplicationService（Knowledge 切片管理应用服务）。
 *
 * <p>该服务承载切片人工维护规则和持久化事务，legacy 只负责 DTO 适配。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class KnowledgeChunkManagementApplicationService implements KnowledgeChunkManagementUseCase {

    /** knowledgeDocumentManagementUseCase（文档管理用例）负责查询文档和校验文档归属。 */
    private final KnowledgeDocumentManagementUseCase knowledgeDocumentManagementUseCase;

    /** knowledgeChunkRepository（知识切片仓储）负责切片持久化。 */
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    /** knowledgeDocumentStatusUseCase（文档状态用例）负责切片变更后的重索引状态。 */
    private final KnowledgeDocumentStatusUseCase knowledgeDocumentStatusUseCase;

    /**
     * findChunk（查询切片）用于读取指定文档下的切片。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 可能存在的切片
     */
    @Override
    public Optional<KnowledgeChunkEntity> findChunk(String documentId, String chunkId) {
        requiredDocument(documentId);
        return knowledgeChunkRepository.findByChunkId(chunkId)
                .filter(chunk -> documentId.equals(chunk.getDocumentId()));
    }

    /**
     * createChunk（新增切片）用于人工补充一个知识切片。
     *
     * @param documentId 知识文档编号
     * @param command 新增切片命令
     * @return 已保存切片
     */
    @Override
    @Transactional
    public KnowledgeChunkEntity createChunk(String documentId, CreateChunkCommand command) {
        KnowledgeDocumentEntity document = editableDocument(documentId);
        if (command == null || command.content() == null || command.content().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新增切片内容不能为空");
        }
        String content = command.content().strip();
        Integer chunkIndex = command.chunkIndex();
        if (chunkIndex == null) {
            chunkIndex = nextChunkIndex(documentId);
        }
        LocalDateTime now = LocalDateTime.now();
        KnowledgeChunkEntity chunk = KnowledgeChunkEntity.builder()
                .id(newId("knowledge_chunk_pk"))
                .chunkId(newId("knowledge_chunk"))
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .documentId(documentId)
                .chunkIndex(chunkIndex)
                .titlePath(blankToNull(command.titlePath()))
                .content(content)
                .contentHash(sha256(content))
                .charCount(content.length())
                .tokenCount(estimateTokenCount(content))
                .metadataJson(blankToNull(command.metadataJson()))
                .enabled(command.enabled() == null || command.enabled() ? 1 : 0)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        KnowledgeChunkEntity savedChunk = knowledgeChunkRepository.saveChunk(chunk);
        knowledgeDocumentStatusUseCase.markReindexRequired(documentId);
        return savedChunk;
    }

    /**
     * updateChunk（更新切片）用于人工修改切片正文或元数据。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param command 更新切片命令
     * @return 更新后切片
     */
    @Override
    @Transactional
    public KnowledgeChunkEntity updateChunk(String documentId, String chunkId, UpdateChunkCommand command) {
        editableDocument(documentId);
        KnowledgeChunkEntity chunk = requiredChunk(documentId, chunkId);
        if (command == null || (command.titlePath() == null && command.content() == null && command.metadataJson() == null)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "更新切片至少需要传入一个字段");
        }
        if (command.titlePath() != null) {
            chunk.setTitlePath(blankToNull(command.titlePath()));
        }
        if (command.content() != null) {
            if (command.content().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "切片内容不能为空");
            }
            String content = command.content().strip();
            chunk.setContent(content);
            chunk.setContentHash(sha256(content));
            chunk.setCharCount(content.length());
            chunk.setTokenCount(estimateTokenCount(content));
        }
        if (command.metadataJson() != null) {
            chunk.setMetadataJson(blankToNull(command.metadataJson()));
        }
        KnowledgeChunkEntity savedChunk = knowledgeChunkRepository.updateChunk(chunk);
        knowledgeDocumentStatusUseCase.markReindexRequired(documentId);
        return savedChunk;
    }

    /**
     * setEnabled（设置切片启用状态）用于控制切片是否参与检索。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param enabled 是否启用
     * @return 更新后切片
     */
    @Override
    @Transactional
    public KnowledgeChunkEntity setEnabled(String documentId, String chunkId, boolean enabled) {
        editableDocument(documentId);
        requiredChunk(documentId, chunkId);
        return knowledgeChunkRepository.markEnabled(chunkId, enabled ? 1 : 0);
    }

    /**
     * deleteChunk（删除切片）用于逻辑删除切片并标记文档待重新索引。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 被删除切片编号
     */
    @Override
    @Transactional
    public String deleteChunk(String documentId, String chunkId) {
        editableDocument(documentId);
        requiredChunk(documentId, chunkId);
        knowledgeChunkRepository.markDeleted(chunkId);
        knowledgeDocumentStatusUseCase.markReindexRequired(documentId);
        return chunkId;
    }

    private KnowledgeDocumentEntity editableDocument(String documentId) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        if (KnowledgeDocumentOverallStatus.PROCESSING.value().equals(document.getOverallStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档正在处理中，暂不允许执行切片管理操作");
        }
        return document;
    }

    private KnowledgeDocumentEntity requiredDocument(String documentId) {
        return knowledgeDocumentManagementUseCase.findByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
    }

    private KnowledgeChunkEntity requiredChunk(String documentId, String chunkId) {
        return knowledgeChunkRepository.findByChunkId(chunkId)
                .filter(chunk -> documentId.equals(chunk.getDocumentId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到当前文档下的知识切片，chunkId：" + chunkId));
    }

    private int nextChunkIndex(String documentId) {
        return knowledgeChunkRepository.listByDocumentId(documentId).stream()
                .map(KnowledgeChunkEntity::getChunkIndex)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(value -> value + 1)
                .orElse(0);
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256 哈希算法不可用", exception);
        }
    }

    private int estimateTokenCount(String text) {
        return text == null || text.isBlank() ? 0 : Math.max(1, (int) Math.ceil(text.length() / 2.0));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

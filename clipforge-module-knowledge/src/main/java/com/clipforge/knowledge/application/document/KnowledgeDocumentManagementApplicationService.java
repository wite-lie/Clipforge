package com.clipforge.knowledge.application.document;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentOverallStatus;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeChunkRepository;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeDocumentManagementApplicationService（Knowledge 文档管理应用服务）。
 *
 * <p>该服务承载不依赖 HTTP 的文档管理操作。删除文档时，
 * 文档和其全部切片在同一个事务中逻辑删除，避免 legacy API 层自行拼接跨表操作。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class KnowledgeDocumentManagementApplicationService implements KnowledgeDocumentManagementUseCase {

    /** knowledgeDocumentRepository（知识文档仓储）负责文档查询和状态更新。 */
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    /** knowledgeChunkRepository（知识切片仓储）负责级联逻辑删除文档切片。 */
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    /**
     * findByDocumentId（按文档编号查询）用于读取未逻辑删除的文档。
     *
     * @param documentId 知识文档编号
     * @return 可能存在的知识文档
     */
    @Override
    public Optional<KnowledgeDocumentEntity> findByDocumentId(String documentId) {
        return knowledgeDocumentRepository.findByDocumentId(documentId);
    }

    /**
     * listByKnowledgeBaseId（按知识库编号查询）用于读取知识库下的文档。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 文档列表
     */
    @Override
    public List<KnowledgeDocumentEntity> listByKnowledgeBaseId(String knowledgeBaseId) {
        return knowledgeDocumentRepository.listByKnowledgeBaseId(knowledgeBaseId);
    }

    /**
     * setEnabled（设置启用状态）用于切换文档是否参与检索。
     *
     * @param documentId 知识文档编号
     * @param enabled 是否启用
     * @return 更新后的文档
     */
    @Override
    public KnowledgeDocumentEntity setEnabled(String documentId, boolean enabled) {
        assertDocumentEditable(documentId);
        return knowledgeDocumentRepository.markEnabled(documentId, enabled ? 1 : 0);
    }

    /**
     * deleteDocument（删除文档）用于在同一事务中逻辑删除文档和全部切片。
     *
     * @param documentId 知识文档编号
     * @return 被删除的文档编号
     */
    @Override
    @Transactional
    public String deleteDocument(String documentId) {
        assertDocumentEditable(documentId);
        knowledgeChunkRepository.markDeletedByDocumentId(documentId);
        knowledgeDocumentRepository.markDeleted(documentId);
        return documentId;
    }

    /**
     * assertDocumentEditable（校验文档可管理）用于阻止处理中的文档被启停或删除。
     *
     * @param documentId 知识文档编号
     */
    private void assertDocumentEditable(String documentId) {
        KnowledgeDocumentEntity document = knowledgeDocumentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
        if (KnowledgeDocumentOverallStatus.PROCESSING.value().equals(document.getOverallStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档正在处理中，暂不允许执行管理操作");
        }
    }
}

package com.clipforge.knowledge.application.processing;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessPhase;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessTriggerType;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentProcessLogRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * KnowledgeDocumentProcessLogApplicationService（知识文档处理日志应用服务）。
 *
 * <p>该服务是 KnowledgeDocumentProcessLogUseCase（知识文档处理日志用例）的数据库实现，
 * 负责组装日志实体并调用 Knowledge 模块自己的日志仓储。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class KnowledgeDocumentProcessLogApplicationService implements KnowledgeDocumentProcessLogUseCase {

    /** processLogRepository（处理日志仓储）负责持久化处理日志。 */
    private final KnowledgeDocumentProcessLogRepository processLogRepository;

    @Override
    public KnowledgeDocumentProcessLogEntity createRunningProcessLog(
            KnowledgeDocumentEntity document,
            KnowledgeDocumentProcessTriggerType triggerType) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentProcessLogEntity entity = KnowledgeDocumentProcessLogEntity.builder()
                .id(newId("kdoc_proc_log_pk"))
                .processLogId(newId("kdoc_proc_log"))
                .documentId(document.getDocumentId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .triggerType(triggerType == null
                        ? KnowledgeDocumentProcessTriggerType.MANUAL.value()
                        : triggerType.value())
                .status(KnowledgeDocumentProcessLogStatus.RUNNING.value())
                .phase(KnowledgeDocumentProcessPhase.PARSE_CHUNK.value())
                .parseDurationMs(null)
                .chunkDurationMs(null)
                .embeddingDurationMs(null)
                .persistDurationMs(null)
                .totalDurationMs(null)
                .chunkCount(0)
                .errorCode(null)
                .errorMessage(null)
                .startTime(now)
                .endTime(null)
                .createTime(now)
                .deleted(0)
                .build();
        return processLogRepository.save(entity);
    }

    @Override
    public KnowledgeDocumentProcessLogEntity markSuccess(
            String processLogId,
            String phase,
            Long chunkDurationMs,
            Long embeddingDurationMs,
            Long totalDurationMs,
            Integer chunkCount) {
        return processLogRepository.markSuccess(
                processLogId,
                phase,
                chunkDurationMs,
                embeddingDurationMs,
                totalDurationMs,
                chunkCount);
    }

    @Override
    public KnowledgeDocumentProcessLogEntity markFailed(
            String processLogId,
            String phase,
            Long chunkDurationMs,
            Long embeddingDurationMs,
            Long totalDurationMs,
            String errorCode,
            String errorMessage) {
        return processLogRepository.markFailed(
                processLogId,
                phase,
                chunkDurationMs,
                embeddingDurationMs,
                totalDurationMs,
                errorCode,
                errorMessage);
    }

    /**
     * newId（生成编号）用于生成当前处理日志所需的业务编号。
     *
     * @param prefix 编号前缀
     * @return 带前缀的随机编号
     */
    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

package com.clipforge.quality.record;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.CreationPlanResult;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.evaluation.CreationPlanEvaluationResult;
import com.clipforge.infra.persistence.entity.CreationEvaluationResultEntity;
import com.clipforge.infra.persistence.entity.CreationPlanVersionEntity;
import com.clipforge.infra.persistence.entity.CreationRevisionResultEntity;
import com.clipforge.infra.persistence.entity.ModelCallRecordEntity;
import com.clipforge.infra.persistence.repository.CreationEvaluationResultRepository;
import com.clipforge.infra.persistence.repository.CreationPlanVersionRepository;
import com.clipforge.infra.persistence.repository.CreationRevisionResultRepository;
import com.clipforge.infra.persistence.repository.ModelCallRecordRepository;
import com.clipforge.quality.version.CreationPlanVersionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DatabaseCreationQualityRecorder（数据库创作质量记录器）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，把方案版本、评估结果和修正关系保存到 PostgreSQL（关系数据库）。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class DatabaseCreationQualityRecorder implements CreationQualityRecorder {

    /** creationPlanVersionRepository（创作方案版本仓储）负责保存和查询版本链。 */
    private final CreationPlanVersionRepository creationPlanVersionRepository;

    /** creationEvaluationResultRepository（创作评估结果仓储）负责保存评估结果。 */
    private final CreationEvaluationResultRepository creationEvaluationResultRepository;

    /** creationRevisionResultRepository（创作修正结果仓储）负责保存修正关系。 */
    private final CreationRevisionResultRepository creationRevisionResultRepository;

    /** modelCallRecordRepository（模型调用记录仓储）用于关联最近一次对应用途的模型调用记录。 */
    private final ModelCallRecordRepository modelCallRecordRepository;

    /** objectMapper（对象映射器）用于把方案和评估结果序列化为 JSON（对象表示格式）。 */
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public String recordInitialVersion(AgentContext context, CreationPlanResult result) {
        return saveVersion(context, result, CreationPlanVersionType.INITIAL, null, "CREATION_PLAN_GENERATION");
    }

    @Override
    @Transactional
    public String recordRevisedVersion(AgentContext context, CreationPlanResult result, String sourceVersionId) {
        return saveVersion(context, result, CreationPlanVersionType.REVISED, sourceVersionId, "CREATION_PLAN_REVISION");
    }

    @Override
    @Transactional
    public String recordEvaluation(AgentContext context, String versionId, CreationPlanEvaluationResult evaluationResult) {
        if (versionId == null || versionId.isBlank() || evaluationResult == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        CreationEvaluationResultEntity entity = CreationEvaluationResultEntity.builder()
                .id(newId("evaluation_pk"))
                .evaluationId(newId("evaluation"))
                .runId(context.getRunId())
                .versionId(versionId)
                .evaluatorType(normalizeEvaluatorType(evaluationResult.getEvaluatorType()))
                .score(evaluationResult.getOverallScore())
                .dimensionScoresJson(toJson(evaluationResult.getDimensionScores()))
                .strengthsJson(toJson(evaluationResult.getStrengths()))
                .weaknessesJson(toJson(evaluationResult.getWeaknesses()))
                .revisionRequired(evaluationResult.isRevisionRequired())
                .revisionAdviceJson(toJson(evaluationResult.getRevisionAdvice()))
                .passed(!evaluationResult.isRevisionRequired())
                .modelCallRecordId(latestModelCallRecordId(context.getRunId(), "CREATION_PLAN_EVALUATION"))
                .createTime(now)
                .deleted(0)
                .build();
        creationEvaluationResultRepository.save(entity);
        return entity.getEvaluationId();
    }

    @Override
    @Transactional
    public String recordRevision(AgentContext context, String sourceVersionId, String targetVersionId, String evaluationId, CreationPlanEvaluationResult evaluationResult) {
        if (sourceVersionId == null || sourceVersionId.isBlank() || targetVersionId == null || targetVersionId.isBlank()) {
            return null;
        }
        CreationRevisionResultEntity entity = CreationRevisionResultEntity.builder()
                .id(newId("revision_pk"))
                .revisionId(newId("revision"))
                .runId(context.getRunId())
                .sourceVersionId(sourceVersionId)
                .targetVersionId(targetVersionId)
                .evaluationId(evaluationId)
                .revisionReason(buildRevisionReason(evaluationResult))
                .revisionSummary(buildRevisionSummary(evaluationResult))
                .modelCallRecordId(latestModelCallRecordId(context.getRunId(), "CREATION_PLAN_REVISION"))
                .createTime(LocalDateTime.now())
                .deleted(0)
                .build();
        creationRevisionResultRepository.save(entity);
        return entity.getRevisionId();
    }

    @Override
    @Transactional
    public void markFinalVersion(String versionId) {
        if (versionId == null || versionId.isBlank()) {
            return;
        }
        creationPlanVersionRepository.markFinal(versionId);
    }

    /**
     * saveVersion（保存版本）用于组装并保存创作方案版本实体。
     */
    private String saveVersion(AgentContext context, CreationPlanResult result, CreationPlanVersionType versionType, String sourceVersionId, String purpose) {
        if (result == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        CreationPlanVersionEntity entity = CreationPlanVersionEntity.builder()
                .id(newId("version_pk"))
                .versionId(newId("version"))
                .runId(context.getRunId())
                .taskId(asString(context.getArtifact("TASK_ID")))
                .projectId(asString(context.getArtifact("PROJECT_ID")))
                .conversationId(asString(context.getArtifact("CONVERSATION_ID")))
                .versionNo(creationPlanVersionRepository.nextVersionNo(context.getRunId()))
                .versionType(versionType.name())
                .sourceVersionId(sourceVersionId)
                .planJson(toJson(result))
                .score(result.getScore())
                .createdByPurpose(purpose)
                .modelCallRecordId(latestModelCallRecordId(context.getRunId(), purpose))
                .finalVersion(false)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        creationPlanVersionRepository.save(entity);
        return entity.getVersionId();
    }

    /**
     * latestModelCallRecordId（最近模型调用记录编号）用于关联当前运行中指定用途的最后一次模型调用。
     */
    private String latestModelCallRecordId(String runId, String purpose) {
        List<ModelCallRecordEntity> records = modelCallRecordRepository.listByRunId(runId);
        for (int index = records.size() - 1; index >= 0; index--) {
            ModelCallRecordEntity record = records.get(index);
            if (purpose.equals(record.getPurpose())) {
                return record.getRecordId();
            }
        }
        return null;
    }

    /**
     * buildRevisionReason（构建修正原因）用于从评估不足中提取可读原因。
     */
    private String buildRevisionReason(CreationPlanEvaluationResult evaluationResult) {
        if (evaluationResult == null || evaluationResult.getWeaknesses() == null || evaluationResult.getWeaknesses().isEmpty()) {
            return "评估结果低于质量阈值或要求修正。";
        }
        return String.join("；", evaluationResult.getWeaknesses());
    }

    /**
     * buildRevisionSummary（构建修正摘要）用于把修正建议保存为可复盘摘要。
     */
    private String buildRevisionSummary(CreationPlanEvaluationResult evaluationResult) {
        if (evaluationResult == null || evaluationResult.getRevisionAdvice() == null || evaluationResult.getRevisionAdvice().isEmpty()) {
            return "根据评估结果进行创作方案修正。";
        }
        return String.join("；", evaluationResult.getRevisionAdvice());
    }

    /**
     * normalizeEvaluatorType（规范化评估器类型）用于统一旧值和空值。
     */
    private String normalizeEvaluatorType(String evaluatorType) {
        if (evaluatorType == null || evaluatorType.isBlank()) {
            return "UNKNOWN";
        }
        if ("llm".equalsIgnoreCase(evaluatorType)) {
            return "LLM_JUDGE";
        }
        if ("rule".equalsIgnoreCase(evaluatorType)) {
            return "RULE";
        }
        return evaluatorType.toUpperCase();
    }

    /**
     * asString（转字符串）用于安全读取上下文元数据。
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * toJson（转 JSON）用于把结构化对象保存为 JSON（对象表示格式）字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创作质量记录 JSON 序列化失败", exception);
        }
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     */
    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}

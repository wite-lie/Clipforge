package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.infra.persistence.entity.ModelCallRecordEntity;
import com.clipforge.infra.persistence.mapper.ModelCallRecordMapper;
import com.clipforge.infra.persistence.repository.ModelCallRecordRepository;
import com.clipforge.model.usage.ModelCallRecord;
import com.clipforge.model.usage.ModelCallRecordQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisModelCallRecordRepository（模型调用记录仓库）。
 *
 * <p>负责把领域层的模型调用记录写入 PostgreSQL，并提供按 runId 和条件查询的能力。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisModelCallRecordRepository implements ModelCallRecordRepository {

    /** 未知运行编号。 */
    private static final String UNKNOWN_RUN_ID = "unknown-run";

    /** 模型调用记录 Mapper。 */
    private final ModelCallRecordMapper modelCallRecordMapper;

    @Override
    public ModelCallRecordEntity save(ModelCallRecord record) {
        ModelCallRecordEntity entity = toEntity(record);
        modelCallRecordMapper.insert(entity);
        return entity;
    }

    @Override
    public List<ModelCallRecordEntity> listByRunId(String runId) {
        String normalizedRunId = normalizeRunId(runId);
        LambdaQueryWrapper<ModelCallRecordEntity> queryWrapper = new LambdaQueryWrapper<ModelCallRecordEntity>()
                .eq(ModelCallRecordEntity::getRunId, normalizedRunId)
                .eq(ModelCallRecordEntity::getDeleted, 0)
                .orderByAsc(ModelCallRecordEntity::getCreateTime);
        return modelCallRecordMapper.selectList(queryWrapper);
    }

    @Override
    public List<ModelCallRecordEntity> listByQuery(ModelCallRecordQuery query) {
        LambdaQueryWrapper<ModelCallRecordEntity> queryWrapper = new LambdaQueryWrapper<ModelCallRecordEntity>()
                .eq(ModelCallRecordEntity::getDeleted, 0);
        if (hasText(query == null ? null : query.getRunId())) {
            queryWrapper.eq(ModelCallRecordEntity::getRunId, normalizeRunId(query.getRunId()));
        }
        if (hasText(query == null ? null : query.getProjectId())) {
            queryWrapper.eq(ModelCallRecordEntity::getProjectId, query.getProjectId());
        }
        if (hasText(query == null ? null : query.getConversationId())) {
            queryWrapper.eq(ModelCallRecordEntity::getConversationId, query.getConversationId());
        }
        if (hasText(query == null ? null : query.getProvider())) {
            queryWrapper.eq(ModelCallRecordEntity::getProvider, query.getProvider());
        }
        if (hasText(query == null ? null : query.getModelName())) {
            queryWrapper.eq(ModelCallRecordEntity::getModelName, query.getModelName());
        }
        if (hasText(query == null ? null : query.getPurpose())) {
            queryWrapper.eq(ModelCallRecordEntity::getPurpose, query.getPurpose());
        }
        if (query != null && query.getSuccess() != null) {
            queryWrapper.eq(ModelCallRecordEntity::getSuccess, query.getSuccess());
        }
        if (query != null && query.getStartTime() != null) {
            queryWrapper.ge(ModelCallRecordEntity::getCreateTime, query.getStartTime());
        }
        if (query != null && query.getEndTime() != null) {
            queryWrapper.le(ModelCallRecordEntity::getCreateTime, query.getEndTime());
        }
        queryWrapper.orderByAsc(ModelCallRecordEntity::getCreateTime);
        return modelCallRecordMapper.selectList(queryWrapper);
    }

    private ModelCallRecordEntity toEntity(ModelCallRecord record) {
        return ModelCallRecordEntity.builder()
                .id(newId("model_call_pk"))
                .recordId(newId("model_call"))
                .runId(normalizeRunId(record == null ? null : record.getRunId()))
                .projectId(record == null ? null : record.getProjectId())
                .conversationId(record == null ? null : record.getConversationId())
                .provider(record == null ? null : record.getProvider())
                .modelName(record == null ? null : record.getModelName())
                .purpose(record == null ? null : record.getPurpose())
                .promptTokens(record == null ? null : record.getPromptTokens())
                .completionTokens(record == null ? null : record.getCompletionTokens())
                .totalTokens(record == null ? null : record.getTotalTokens())
                .latencyMs(record == null ? null : record.getLatencyMs())
                .success(record == null ? null : record.getSuccess())
                .errorCode(record == null ? null : record.getErrorCode())
                .errorMessage(record == null ? null : record.getErrorMessage())
                .createTime(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    private String normalizeRunId(String runId) {
        if (runId == null || runId.isBlank()) {
            return UNKNOWN_RUN_ID;
        }
        return runId.trim();
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

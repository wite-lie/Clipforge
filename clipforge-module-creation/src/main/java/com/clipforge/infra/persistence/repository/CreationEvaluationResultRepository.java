package com.clipforge.memory.infrastructure.persistence.repository;

import com.clipforge.memory.infrastructure.persistence.entity.CreationEvaluationResultEntity;
import java.util.List;

/**
 * CreationEvaluationResultRepository（创作评估结果仓储）。
 *
 * <p>该仓储负责保存和查询结构化评估结果。
 */
public interface CreationEvaluationResultRepository {

    /**
     * save（保存）用于保存一个评估结果。
     *
     * @param entity 评估结果实体
     * @return 已保存实体
     */
    CreationEvaluationResultEntity save(CreationEvaluationResultEntity entity);

    /**
     * listByRunId（按运行编号查询）用于查询一次运行的全部评估结果。
     *
     * @param runId 运行编号
     * @return 评估结果列表
     */
    List<CreationEvaluationResultEntity> listByRunId(String runId);
}

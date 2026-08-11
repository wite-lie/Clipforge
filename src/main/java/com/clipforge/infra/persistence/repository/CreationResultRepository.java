package com.clipforge.infra.persistence.repository;

import com.clipforge.agent.CreationPlanResult;
import com.clipforge.infra.persistence.entity.CreationResultEntity;
import java.util.Optional;

/**
 * CreationResultRepository（创作结果仓储）。
 *
 * <p>该接口负责保存和查询 Agent（智能体）生成的结构化创作方案。
 */
public interface CreationResultRepository {

    /**
     * saveResult（保存结果）用于把创作方案结果保存到数据库。
     *
     * @param taskId 创作任务编号
     * @param runId Agent（智能体）运行编号
     * @param result 创作方案结果
     * @return 已保存的创作结果实体
     */
    CreationResultEntity saveResult(String taskId, String runId, CreationPlanResult result);

    /**
     * findByRunId（按运行编号查询）用于查询一次 Agent（智能体）运行的创作结果。
     *
     * @param runId Agent（智能体）运行编号
     * @return 可能存在的创作结果实体
     */
    Optional<CreationResultEntity> findByRunId(String runId);
}

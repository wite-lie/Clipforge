package com.clipforge.memory.infrastructure.persistence.repository;

import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.memory.infrastructure.persistence.entity.CreationOutputEntity;
import java.util.Optional;

/**
 * CreationOutputRepository（创作产物仓储）。
 *
 * <p>该接口负责保存和查询通用创作产物。
 */
public interface CreationOutputRepository {

    /**
     * saveOutput（保存创作产物）用于把一次创作运行的最终结构化产物保存到通用产物表。
     *
     * @param taskId 创作任务编号
     * @param runId 运行编号
     * @param outputType 产物类型
     * @param outputPayload 当前阶段生成的真实结构化产物
     * @return 已保存的创作产物实体
     */
    CreationOutputEntity saveOutput(String taskId, String runId, CreationOutputType outputType, Object outputPayload);

    /**
     * findLatestByRunId（按运行编号查询最新产物）用于读取一次创作运行最终保存的通用产物。
     *
     * @param runId 运行编号
     * @return 可能存在的创作产物实体
     */
    Optional<CreationOutputEntity> findLatestByRunId(String runId);
}

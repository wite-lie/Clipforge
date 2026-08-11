package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.CreationPlanVersionEntity;
import java.util.List;

/**
 * CreationPlanVersionRepository（创作方案版本仓储）。
 *
 * <p>该仓储负责保存和查询创作方案版本链路。
 */
public interface CreationPlanVersionRepository {

    /**
     * save（保存）用于保存一个创作方案版本。
     *
     * @param entity 创作方案版本实体
     * @return 已保存实体
     */
    CreationPlanVersionEntity save(CreationPlanVersionEntity entity);

    /**
     * nextVersionNo（下一个版本序号）用于计算某次运行下的新版本序号。
     *
     * @param runId 运行编号
     * @return 下一个版本序号
     */
    int nextVersionNo(String runId);

    /**
     * markFinal（标记最终版本）用于把某个版本标记为本次运行最终版本。
     *
     * @param versionId 版本编号
     */
    void markFinal(String versionId);

    /**
     * listByRunId（按运行编号查询）用于查询一次运行的全部方案版本。
     *
     * @param runId 运行编号
     * @return 方案版本列表
     */
    List<CreationPlanVersionEntity> listByRunId(String runId);
}

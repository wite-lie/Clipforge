package com.clipforge.memory.infrastructure.persistence.repository;

import com.clipforge.memory.infrastructure.persistence.entity.CreationRevisionResultEntity;
import java.util.List;

/**
 * CreationRevisionResultRepository（创作修正结果仓储）。
 *
 * <p>该仓储负责保存和查询修正前后版本关系。
 */
public interface CreationRevisionResultRepository {

    /**
     * save（保存）用于保存一个修正记录。
     *
     * @param entity 修正结果实体
     * @return 已保存实体
     */
    CreationRevisionResultEntity save(CreationRevisionResultEntity entity);

    /**
     * listByRunId（按运行编号查询）用于查询一次运行的全部修正记录。
     *
     * @param runId 运行编号
     * @return 修正记录列表
     */
    List<CreationRevisionResultEntity> listByRunId(String runId);
}

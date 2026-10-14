package com.clipforge.memory.infrastructure.persistence.repository;

import com.clipforge.memory.infrastructure.persistence.entity.ModelCallRecordEntity;
import com.clipforge.observability.usage.ModelCallRecord;
import com.clipforge.observability.usage.ModelCallRecordQuery;
import java.util.List;

/**
 * ModelCallRecordRepository（模型调用记录仓储）。
 *
 * <p>该接口负责保存和查询模型调用明细。
 */
public interface ModelCallRecordRepository {

    /**
     * save（保存）用于保存一条模型调用记录。
     *
     * @param record 模型调用记录
     * @return 已保存的模型调用记录实体
     */
    ModelCallRecordEntity save(ModelCallRecord record);

    /**
     * listByRunId（按运行编号查询列表）用于查看一次 Agent（智能体）运行的全部模型调用。
     *
     * @param runId 运行编号
     * @return 模型调用记录实体列表
     */
    List<ModelCallRecordEntity> listByRunId(String runId);

    /**
     * listByQuery（按条件查询列表）用于模型用量和成本统计。
     *
     * @param query 查询条件
     * @return 模型调用记录实体列表
     */
    List<ModelCallRecordEntity> listByQuery(ModelCallRecordQuery query);
}

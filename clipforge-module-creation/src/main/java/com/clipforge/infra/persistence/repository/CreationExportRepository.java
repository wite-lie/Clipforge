package com.clipforge.memory.infrastructure.persistence.repository;

import com.clipforge.memory.infrastructure.persistence.entity.CreationExportEntity;
import java.util.Optional;

/**
 * CreationExportRepository（创作导出仓储）。
 */
public interface CreationExportRepository {

    /**
     * save（保存）用于保存导出记录。
     *
     * @param entity 导出实体
     * @return 已保存实体
     */
    CreationExportEntity save(CreationExportEntity entity);

    /**
     * findByExportId（按导出编号查询）用于读取导出记录。
     *
     * @param exportId 导出编号
     * @return 可能存在的导出记录
     */
    Optional<CreationExportEntity> findByExportId(String exportId);

    /**
     * findLatestSucceededByRunId（查询最近成功导出）用于复用已存在的 Markdown 文件。
     *
     * @param runId 运行编号
     * @return 可能存在的导出记录
     */
    Optional<CreationExportEntity> findLatestSucceededByRunId(String runId);
}

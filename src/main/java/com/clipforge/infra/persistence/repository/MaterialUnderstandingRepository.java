package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.MaterialUnderstandingEntity;
import java.util.Optional;

/**
 * MaterialUnderstandingRepository（素材理解结果仓储）。
 *
 * <p>该接口负责保存和查询素材理解结果。
 */
public interface MaterialUnderstandingRepository {

    /**
     * save（保存）用于写入素材理解结果。
     *
     * @param entity 素材理解结果实体
     * @return 已保存实体
     */
    MaterialUnderstandingEntity save(MaterialUnderstandingEntity entity);

    /**
     * findReusable（查询可复用结果）用于按素材、理解类型、模型、提示词版本和内容哈希查询成功结果。
     *
     * @param materialId 素材编号
     * @param understandingType 理解类型
     * @param modelName 模型名称
     * @param promptVersion 提示词版本
     * @param contentHash 内容哈希
     * @return 可能存在的素材理解结果
     */
    Optional<MaterialUnderstandingEntity> findReusable(String materialId, String understandingType, String modelName, String promptVersion, String contentHash);

    /**
     * findLatestSucceeded（查询最近成功结果）用于创作 Workflow（工作流）读取已保存的素材理解结果。
     *
     * @param materialId 素材编号
     * @param understandingType 理解类型
     * @return 可能存在的素材理解结果
     */
    Optional<MaterialUnderstandingEntity> findLatestSucceeded(String materialId, String understandingType);
}

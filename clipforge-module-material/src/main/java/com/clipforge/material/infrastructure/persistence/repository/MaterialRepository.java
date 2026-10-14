package com.clipforge.material.infrastructure.persistence.repository;

import com.clipforge.material.infrastructure.persistence.entity.MaterialEntity;
import com.clipforge.material.MaterialQueryCondition;
import java.util.List;
import java.util.Optional;

/**
 * MaterialRepository（素材仓储）。
 *
 * <p>该接口负责保存和查询 Material（素材）元数据。
 */
public interface MaterialRepository {

    /**
     * save（保存）用于写入素材元数据。
     *
     * @param entity 素材实体
     * @return 已保存素材实体
     */
    MaterialEntity save(MaterialEntity entity);

    /**
     * findByMaterialId（按素材编号查询）用于读取素材详情。
     *
     * @param materialId 素材编号
     * @return 可能存在的素材实体
     */
    Optional<MaterialEntity> findByMaterialId(String materialId);

    /**
     * listByCondition（按条件查询列表）用于读取用户、项目、会话或运行任务下的素材集合。
     *
     * @param condition 素材查询条件
     * @return 素材实体列表
     */
    List<MaterialEntity> listByCondition(MaterialQueryCondition condition);

    /**
     * listByMaterialIds（按素材编号列表查询）用于读取用户明确选择的素材集合。
     *
     * @param materialIds 素材编号列表
     * @param materialType 素材类型，例如 IMAGE（图片）或 VIDEO（视频）
     * @return 素材实体列表
     */
    List<MaterialEntity> listByMaterialIds(List<String> materialIds, String materialType);

    /**
     * logicalDeleteByMaterialIds（按素材编号列表逻辑删除）用于清理系统派生素材。
     *
     * @param materialIds 素材编号列表
     */
    void logicalDeleteByMaterialIds(List<String> materialIds);
}

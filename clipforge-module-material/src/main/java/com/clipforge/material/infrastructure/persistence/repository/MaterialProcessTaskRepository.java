package com.clipforge.material.infrastructure.persistence.repository;

import com.clipforge.material.infrastructure.persistence.entity.MaterialProcessTaskEntity;
import java.util.List;
import java.util.Optional;

/**
 * MaterialProcessTaskRepository（素材处理任务仓储）。
 *
 * <p>该接口负责保存和查询素材处理任务状态。
 */
public interface MaterialProcessTaskRepository {

    /**
     * save（保存）用于写入素材处理任务。
     *
     * @param entity 素材处理任务实体
     * @return 已保存素材处理任务实体
     */
    MaterialProcessTaskEntity save(MaterialProcessTaskEntity entity);

    /**
     * findByProcessTaskId（按处理任务编号查询）用于读取任务状态。
     *
     * @param processTaskId 处理任务编号
     * @return 可能存在的素材处理任务实体
     */
    Optional<MaterialProcessTaskEntity> findByProcessTaskId(String processTaskId);

    /**
     * findByParentProcessTaskId（按父任务编号查询子任务）用于读取视频父任务下的 FRAME_BRANCH（抽帧图像分支）、AUDIO_BRANCH（音频分支）和 SUMMARY（汇总）子任务。
     *
     * @param parentProcessTaskId 父处理任务编号
     * @return 子任务列表
     */
    List<MaterialProcessTaskEntity> findByParentProcessTaskId(String parentProcessTaskId);

    /**
     * findByParentProcessTaskIdAndSubTaskKey（按父任务编号和子任务键查询）用于幂等判断某个视频子任务是否已经创建。
     *
     * @param parentProcessTaskId 父处理任务编号
     * @param subTaskKey 子任务键
     * @return 可能存在的子任务
     */
    Optional<MaterialProcessTaskEntity> findByParentProcessTaskIdAndSubTaskKey(String parentProcessTaskId, String subTaskKey);

    /**
     * findLatestByMaterialId（按素材编号查询最近任务）用于读取某个素材最近一次处理任务。
     *
     * @param materialId 素材编号
     * @return 可能存在的素材处理任务实体
     */
    Optional<MaterialProcessTaskEntity> findLatestByMaterialId(String materialId);

    /**
     * update（更新）用于按主键更新素材处理任务状态。
     *
     * @param entity 素材处理任务实体
     */
    void update(MaterialProcessTaskEntity entity);
}

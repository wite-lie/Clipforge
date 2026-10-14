package com.clipforge.material.task.cache;

import java.util.Optional;

/**
 * MaterialProcessTaskStatusCache（素材处理任务状态缓存）。
 *
 * <p>该接口用于屏蔽 Redis（缓存）实现细节，应用层只依赖该抽象读取、保存和删除素材处理任务状态。
 */
public interface MaterialProcessTaskStatusCache {

    /**
     * get（读取）用于按任务编号读取素材处理任务缓存状态。
     *
     * @param taskId 任务编号
     * @return 可能存在的素材处理任务状态
     */
    Optional<MaterialProcessTaskStatus> get(String taskId);

    /**
     * save（保存）用于写入或刷新素材处理任务缓存状态。
     *
     * @param status 素材处理任务状态
     */
    void save(MaterialProcessTaskStatus status);

    /**
     * delete（删除）用于清理素材处理任务缓存状态。
     *
     * @param taskId 任务编号
     */
    void delete(String taskId);
}

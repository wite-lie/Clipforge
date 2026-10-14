package com.clipforge.material.task.cache;

import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * NoOpMaterialProcessTaskStatusCache（空素材处理任务状态缓存）。
 *
 * <p>该实现用于没有 Redis（缓存）实现时兜底，保证任务状态查询仍然可以回退 PostgreSQL（关系数据库）。
 */
@Component
public class NoOpMaterialProcessTaskStatusCache implements MaterialProcessTaskStatusCache {

    /**
     * get（读取）用于按任务编号读取素材处理任务缓存状态。
     *
     * @param taskId 任务编号
     * @return 空结果
     */
    @Override
    public Optional<MaterialProcessTaskStatus> get(String taskId) {
        // 空实现永远不命中缓存。
        return Optional.empty();
    }

    /**
     * save（保存）用于写入或刷新素材处理任务缓存状态。
     *
     * @param status 素材处理任务状态
     */
    @Override
    public void save(MaterialProcessTaskStatus status) {
        // 空实现不执行任何写入。
    }

    /**
     * delete（删除）用于清理素材处理任务缓存状态。
     *
     * @param taskId 任务编号
     */
    @Override
    public void delete(String taskId) {
        // 空实现不执行任何删除。
    }
}

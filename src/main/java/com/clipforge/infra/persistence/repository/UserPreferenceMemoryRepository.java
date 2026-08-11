package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.UserPreferenceMemoryEntity;
import java.util.List;
import java.util.Optional;

/**
 * UserPreferenceMemoryRepository（用户偏好记忆仓储）。
 *
 * <p>该接口负责管理用户级 Memory（记忆）的保存、读取和停用。
 */
public interface UserPreferenceMemoryRepository {

    /**
     * save（保存）用于新增用户偏好记忆。
     *
     * @param entity 用户偏好记忆实体
     * @return 已保存实体
     */
    UserPreferenceMemoryEntity save(UserPreferenceMemoryEntity entity);

    /**
     * findByMemoryId（按记忆编号查询）用于查询单条用户偏好记忆。
     *
     * @param memoryId 记忆编号
     * @return 可能存在的用户偏好记忆
     */
    Optional<UserPreferenceMemoryEntity> findByMemoryId(String memoryId);

    /**
     * listActiveByUserId（按用户查询启用记忆）用于读取用户长期偏好。
     *
     * @param userId 用户编号
     * @return 启用中的用户偏好记忆列表
     */
    List<UserPreferenceMemoryEntity> listActiveByUserId(String userId);

    /**
     * listActiveByUserIdAndKey（按用户和键查询启用记忆）用于读取某个偏好维度。
     *
     * @param userId 用户编号
     * @param memoryKey 记忆键
     * @return 启用中的用户偏好记忆列表
     */
    List<UserPreferenceMemoryEntity> listActiveByUserIdAndKey(String userId, String memoryKey);

    /**
     * disableByMemoryId（按记忆编号停用）用于人工修正或冲突覆盖旧记忆。
     *
     * @param memoryId 记忆编号
     * @return 是否更新成功
     */
    boolean disableByMemoryId(String memoryId);
}

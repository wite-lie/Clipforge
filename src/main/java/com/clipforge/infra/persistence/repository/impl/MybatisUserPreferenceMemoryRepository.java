package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.UserPreferenceMemoryEntity;
import com.clipforge.infra.persistence.enums.MemoryStatus;
import com.clipforge.infra.persistence.mapper.UserPreferenceMemoryMapper;
import com.clipforge.infra.persistence.repository.UserPreferenceMemoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisUserPreferenceMemoryRepository（MyBatis 用户偏好记忆仓储实现）。
 *
 * <p>该实现仅在 db（数据库）Profile（配置环境）下启用。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisUserPreferenceMemoryRepository implements UserPreferenceMemoryRepository {

    /** userPreferenceMemoryMapper（用户偏好记忆映射器）负责访问用户偏好记忆表。 */
    private final UserPreferenceMemoryMapper userPreferenceMemoryMapper;

    @Override
    public UserPreferenceMemoryEntity save(UserPreferenceMemoryEntity entity) {
        userPreferenceMemoryMapper.insert(entity);
        return entity;
    }

    @Override
    public Optional<UserPreferenceMemoryEntity> findByMemoryId(String memoryId) {
        LambdaQueryWrapper<UserPreferenceMemoryEntity> queryWrapper = new LambdaQueryWrapper<UserPreferenceMemoryEntity>()
                .eq(UserPreferenceMemoryEntity::getMemoryId, memoryId)
                .eq(UserPreferenceMemoryEntity::getDeleted, 0);
        return Optional.ofNullable(userPreferenceMemoryMapper.selectOne(queryWrapper));
    }

    @Override
    public List<UserPreferenceMemoryEntity> listActiveByUserId(String userId) {
        LambdaQueryWrapper<UserPreferenceMemoryEntity> queryWrapper = new LambdaQueryWrapper<UserPreferenceMemoryEntity>()
                .eq(UserPreferenceMemoryEntity::getUserId, userId)
                .eq(UserPreferenceMemoryEntity::getStatus, MemoryStatus.ACTIVE.value())
                .eq(UserPreferenceMemoryEntity::getDeleted, 0)
                .orderByDesc(UserPreferenceMemoryEntity::getUpdateTime);
        return userPreferenceMemoryMapper.selectList(queryWrapper);
    }

    @Override
    public List<UserPreferenceMemoryEntity> listActiveByUserIdAndKey(String userId, String memoryKey) {
        LambdaQueryWrapper<UserPreferenceMemoryEntity> queryWrapper = new LambdaQueryWrapper<UserPreferenceMemoryEntity>()
                .eq(UserPreferenceMemoryEntity::getUserId, userId)
                .eq(UserPreferenceMemoryEntity::getMemoryKey, memoryKey)
                .eq(UserPreferenceMemoryEntity::getStatus, MemoryStatus.ACTIVE.value())
                .eq(UserPreferenceMemoryEntity::getDeleted, 0)
                .orderByDesc(UserPreferenceMemoryEntity::getUpdateTime);
        return userPreferenceMemoryMapper.selectList(queryWrapper);
    }

    @Override
    public boolean disableByMemoryId(String memoryId) {
        LambdaUpdateWrapper<UserPreferenceMemoryEntity> updateWrapper = new LambdaUpdateWrapper<UserPreferenceMemoryEntity>()
                .set(UserPreferenceMemoryEntity::getStatus, MemoryStatus.DISABLED.value())
                .set(UserPreferenceMemoryEntity::getUpdateTime, LocalDateTime.now())
                .eq(UserPreferenceMemoryEntity::getMemoryId, memoryId)
                .eq(UserPreferenceMemoryEntity::getDeleted, 0);
        return userPreferenceMemoryMapper.update(null, updateWrapper) > 0;
    }
}

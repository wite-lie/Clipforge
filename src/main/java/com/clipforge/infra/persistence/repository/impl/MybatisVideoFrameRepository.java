package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.VideoFrameEntity;
import com.clipforge.infra.persistence.mapper.VideoFrameMapper;
import com.clipforge.infra.persistence.repository.VideoFrameRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * MybatisVideoFrameRepository（MyBatis 视频候选帧仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问视频候选帧表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisVideoFrameRepository implements VideoFrameRepository {

    /** videoFrameMapper（视频候选帧映射器）负责执行候选帧表 CRUD（增删改查）。 */
    private final VideoFrameMapper videoFrameMapper;

    /**
     * replaceByMaterialId（按素材编号替换）用于逻辑删除旧帧并保存新帧。
     *
     * @param materialId 素材编号
     * @param entities 新候选帧实体列表
     * @return 保存后的候选帧实体列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<VideoFrameEntity> replaceByMaterialId(String materialId, List<VideoFrameEntity> entities) {
        // 逻辑删除当前视频下的旧候选帧。
        logicalDeleteByMaterialId(materialId);
        // 逐条插入新候选帧。
        for (VideoFrameEntity entity : entities) {
            // 插入候选帧实体。
            videoFrameMapper.insert(entity);
        }
        // 返回保存后的实体列表。
        return entities;
    }

    /**
     * listByMaterialId（按素材编号查询）用于读取候选帧列表。
     *
     * @param materialId 素材编号
     * @return 候选帧实体列表
     */
    @Override
    public List<VideoFrameEntity> listByMaterialId(String materialId) {
        // 创建查询条件。
        LambdaQueryWrapper<VideoFrameEntity> queryWrapper = new LambdaQueryWrapper<VideoFrameEntity>()
                // materialId（素材编号）必须匹配。
                .eq(VideoFrameEntity::getMaterialId, materialId)
                // deleted（逻辑删除）必须为正常。
                .eq(VideoFrameEntity::getDeleted, 0)
                // 按帧序号升序排列。
                .orderByAsc(VideoFrameEntity::getFrameIndex);
        // 执行查询。
        return videoFrameMapper.selectList(queryWrapper);
    }

    /**
     * updateFrameMaterialId（更新帧素材编号）用于把候选帧记录和图片素材记录建立关联。
     *
     * @param frameId 候选帧编号
     * @param frameMaterialId 帧图片素材编号
     */
    @Override
    public void updateFrameMaterialId(String frameId, String frameMaterialId) {
        // 创建更新条件。
        LambdaUpdateWrapper<VideoFrameEntity> updateWrapper = new LambdaUpdateWrapper<VideoFrameEntity>()
                // 设置帧图片素材编号。
                .set(VideoFrameEntity::getFrameMaterialId, frameMaterialId)
                // 更新时间。
                .set(VideoFrameEntity::getUpdateTime, LocalDateTime.now())
                // 限定候选帧编号。
                .eq(VideoFrameEntity::getFrameId, frameId)
                // 只处理未删除记录。
                .eq(VideoFrameEntity::getDeleted, 0);
        // 执行更新。
        videoFrameMapper.update(null, updateWrapper);
    }

    /**
     * logicalDeleteByMaterialId（按素材编号逻辑删除）用于重抽帧前清理旧记录。
     *
     * @param materialId 素材编号
     */
    private void logicalDeleteByMaterialId(String materialId) {
        // 创建更新条件。
        LambdaUpdateWrapper<VideoFrameEntity> updateWrapper = new LambdaUpdateWrapper<VideoFrameEntity>()
                // 设置删除标记。
                .set(VideoFrameEntity::getDeleted, 1)
                // 更新时间。
                .set(VideoFrameEntity::getUpdateTime, LocalDateTime.now())
                // 限定素材编号。
                .eq(VideoFrameEntity::getMaterialId, materialId)
                // 只处理未删除记录。
                .eq(VideoFrameEntity::getDeleted, 0);
        // 执行逻辑删除。
        videoFrameMapper.update(null, updateWrapper);
    }
}

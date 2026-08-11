package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.VideoUnderstandingEntity;
import com.clipforge.infra.persistence.mapper.VideoUnderstandingMapper;
import com.clipforge.infra.persistence.repository.VideoUnderstandingRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisVideoUnderstandingRepository（MyBatis 视频理解仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问视频理解结果表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisVideoUnderstandingRepository implements VideoUnderstandingRepository {

    /** videoUnderstandingMapper（视频理解映射器）负责执行视频理解结果表 CRUD（增删改查）。 */
    private final VideoUnderstandingMapper videoUnderstandingMapper;

    /**
     * saveOrUpdateByMaterialId（按素材编号保存或更新）用于写入视频理解结果。
     *
     * @param entity 视频理解实体
     * @return 保存后的实体
     */
    @Override
    public VideoUnderstandingEntity saveOrUpdateByMaterialId(VideoUnderstandingEntity entity) {
        // 按素材编号查询已有视频理解记录。
        Optional<VideoUnderstandingEntity> existing = findByMaterialId(entity.getMaterialId());
        // 如果不存在，则插入新记录。
        if (existing.isEmpty()) {
            // 插入视频理解记录。
            videoUnderstandingMapper.insert(entity);
            // 返回插入实体。
            return entity;
        }
        // 读取已有记录。
        VideoUnderstandingEntity existingEntity = existing.get();
        // 保留已有数据库主键。
        entity.setId(existingEntity.getId());
        // 保留已有业务编号。
        entity.setUnderstandingId(existingEntity.getUnderstandingId());
        // 保留已有创建时间。
        entity.setCreateTime(existingEntity.getCreateTime());
        // 设置更新时间。
        entity.setUpdateTime(LocalDateTime.now());
        // 创建更新条件，并显式设置所有快照字段。
        LambdaUpdateWrapper<VideoUnderstandingEntity> updateWrapper = new LambdaUpdateWrapper<VideoUnderstandingEntity>()
                .set(VideoUnderstandingEntity::getVideoMetadataSnapshot, entity.getVideoMetadataSnapshot())
                .set(VideoUnderstandingEntity::getFrameSummarySnapshot, entity.getFrameSummarySnapshot())
                .set(VideoUnderstandingEntity::getAudioTranscriptSnapshot, entity.getAudioTranscriptSnapshot())
                .set(VideoUnderstandingEntity::getUnderstandingJson, entity.getUnderstandingJson())
                .set(VideoUnderstandingEntity::getUnderstandingStatus, entity.getUnderstandingStatus())
                .set(VideoUnderstandingEntity::getErrorMessage, entity.getErrorMessage())
                .set(VideoUnderstandingEntity::getUpdateTime, entity.getUpdateTime())
                .eq(VideoUnderstandingEntity::getMaterialId, entity.getMaterialId())
                .eq(VideoUnderstandingEntity::getDeleted, 0);
        // 执行更新。
        videoUnderstandingMapper.update(null, updateWrapper);
        // 返回更新后的实体。
        return entity;
    }

    /**
     * findByMaterialId（按素材编号查询）用于读取视频理解结果。
     *
     * @param materialId 素材编号
     * @return 可能存在的视频理解实体
     */
    @Override
    public Optional<VideoUnderstandingEntity> findByMaterialId(String materialId) {
        // 创建查询条件。
        LambdaQueryWrapper<VideoUnderstandingEntity> queryWrapper = new LambdaQueryWrapper<VideoUnderstandingEntity>()
                .eq(VideoUnderstandingEntity::getMaterialId, materialId)
                .eq(VideoUnderstandingEntity::getDeleted, 0);
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(videoUnderstandingMapper.selectOne(queryWrapper));
    }
}

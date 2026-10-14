package com.clipforge.material.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.material.infrastructure.persistence.entity.VideoMetadataEntity;
import com.clipforge.material.infrastructure.persistence.mapper.VideoMetadataMapper;
import com.clipforge.material.infrastructure.persistence.repository.VideoMetadataRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisVideoMetadataRepository（MyBatis 视频元数据仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问视频元数据表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisVideoMetadataRepository implements VideoMetadataRepository {

    /** videoMetadataMapper（视频元数据映射器）负责执行视频元数据表 CRUD（增删改查）。 */
    private final VideoMetadataMapper videoMetadataMapper;

    /**
     * saveOrUpdateByMaterialId（按素材编号保存或更新）用于写入视频元数据。
     *
     * @param entity 视频元数据实体
     * @return 保存后的实体
     */
    @Override
    public VideoMetadataEntity saveOrUpdateByMaterialId(VideoMetadataEntity entity) {
        // 按素材编号查询已有元数据。
        Optional<VideoMetadataEntity> existing = findByMaterialId(entity.getMaterialId());
        // 如果不存在，则插入新记录。
        if (existing.isEmpty()) {
            // 插入视频元数据。
            videoMetadataMapper.insert(entity);
            // 返回插入实体。
            return entity;
        }
        // 读取已有记录。
        VideoMetadataEntity existingEntity = existing.get();
        // 保留已有数据库主键。
        entity.setId(existingEntity.getId());
        // 保留已有业务编号。
        entity.setMetadataId(existingEntity.getMetadataId());
        // 保留已有创建时间。
        entity.setCreateTime(existingEntity.getCreateTime());
        // 设置更新时间。
        entity.setUpdateTime(LocalDateTime.now());
        // 创建更新条件，并显式设置每个字段，确保失败状态可以清空旧的成功元数据。
        LambdaUpdateWrapper<VideoMetadataEntity> updateWrapper = new LambdaUpdateWrapper<VideoMetadataEntity>()
                // 设置时长。
                .set(VideoMetadataEntity::getDurationSeconds, entity.getDurationSeconds())
                // 设置宽度。
                .set(VideoMetadataEntity::getWidth, entity.getWidth())
                // 设置高度。
                .set(VideoMetadataEntity::getHeight, entity.getHeight())
                // 设置帧率。
                .set(VideoMetadataEntity::getFrameRate, entity.getFrameRate())
                // 设置码率。
                .set(VideoMetadataEntity::getBitRate, entity.getBitRate())
                // 设置格式名称。
                .set(VideoMetadataEntity::getFormatName, entity.getFormatName())
                // 设置视频编码。
                .set(VideoMetadataEntity::getVideoCodec, entity.getVideoCodec())
                // 设置是否有音轨。
                .set(VideoMetadataEntity::getHasAudio, entity.getHasAudio())
                // 设置音频编码。
                .set(VideoMetadataEntity::getAudioCodec, entity.getAudioCodec())
                // 设置音轨数量。
                .set(VideoMetadataEntity::getAudioStreamCount, entity.getAudioStreamCount())
                // 设置探测状态。
                .set(VideoMetadataEntity::getProbeStatus, entity.getProbeStatus())
                // 设置探测错误消息。
                .set(VideoMetadataEntity::getProbeErrorMessage, entity.getProbeErrorMessage())
                // 设置原始探测 JSON。
                .set(VideoMetadataEntity::getRawProbeJson, entity.getRawProbeJson())
                // 设置更新时间。
                .set(VideoMetadataEntity::getUpdateTime, entity.getUpdateTime())
                // 按素材编号更新。
                .eq(VideoMetadataEntity::getMaterialId, entity.getMaterialId())
                // 只更新未删除记录。
                .eq(VideoMetadataEntity::getDeleted, 0);
        // 执行更新。
        videoMetadataMapper.update(null, updateWrapper);
        // 返回更新后的实体。
        return entity;
    }

    /**
     * findByMaterialId（按素材编号查询）用于读取视频元数据。
     *
     * @param materialId 素材编号
     * @return 可能存在的视频元数据
     */
    @Override
    public Optional<VideoMetadataEntity> findByMaterialId(String materialId) {
        // 创建查询条件。
        LambdaQueryWrapper<VideoMetadataEntity> queryWrapper = new LambdaQueryWrapper<VideoMetadataEntity>()
                // materialId（素材编号）必须匹配。
                .eq(VideoMetadataEntity::getMaterialId, materialId)
                // deleted（逻辑删除）必须为正常。
                .eq(VideoMetadataEntity::getDeleted, 0);
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(videoMetadataMapper.selectOne(queryWrapper));
    }
}

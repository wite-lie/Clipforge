package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.VideoSubtitleSegmentEntity;
import com.clipforge.infra.persistence.mapper.VideoSubtitleSegmentMapper;
import com.clipforge.infra.persistence.repository.VideoSubtitleSegmentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * MybatisVideoSubtitleSegmentRepository（MyBatis 视频字幕片段仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisVideoSubtitleSegmentRepository implements VideoSubtitleSegmentRepository {

    /** videoSubtitleSegmentMapper（视频字幕片段映射器）负责执行字幕片段表 CRUD（增删改查）。 */
    private final VideoSubtitleSegmentMapper videoSubtitleSegmentMapper;

    /**
     * replaceByMaterialId（按素材编号替换）用于逻辑删除旧片段并保存新片段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<VideoSubtitleSegmentEntity> replaceByMaterialId(String materialId, List<VideoSubtitleSegmentEntity> segments) {
        logicalDeleteByMaterialId(materialId);
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }
        for (VideoSubtitleSegmentEntity segment : segments) {
            videoSubtitleSegmentMapper.insert(segment);
        }
        return segments;
    }

    /**
     * listByMaterialId（按素材编号查询）用于按片段序号返回字幕时间线。
     */
    @Override
    public List<VideoSubtitleSegmentEntity> listByMaterialId(String materialId) {
        LambdaQueryWrapper<VideoSubtitleSegmentEntity> queryWrapper = new LambdaQueryWrapper<VideoSubtitleSegmentEntity>()
                .eq(VideoSubtitleSegmentEntity::getMaterialId, materialId)
                .eq(VideoSubtitleSegmentEntity::getDeleted, 0)
                .orderByAsc(VideoSubtitleSegmentEntity::getSegmentIndex);
        return videoSubtitleSegmentMapper.selectList(queryWrapper);
    }

    /**
     * logicalDeleteByMaterialId（按素材编号逻辑删除）用于替换字幕片段前清理旧数据。
     */
    private void logicalDeleteByMaterialId(String materialId) {
        LambdaUpdateWrapper<VideoSubtitleSegmentEntity> updateWrapper = new LambdaUpdateWrapper<VideoSubtitleSegmentEntity>()
                .set(VideoSubtitleSegmentEntity::getDeleted, 1)
                .set(VideoSubtitleSegmentEntity::getUpdateTime, LocalDateTime.now())
                .eq(VideoSubtitleSegmentEntity::getMaterialId, materialId)
                .eq(VideoSubtitleSegmentEntity::getDeleted, 0);
        videoSubtitleSegmentMapper.update(null, updateWrapper);
    }
}

package com.clipforge.material.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.material.infrastructure.persistence.entity.VideoAudioEntity;
import com.clipforge.material.infrastructure.persistence.mapper.VideoAudioMapper;
import com.clipforge.material.infrastructure.persistence.repository.VideoAudioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisVideoAudioRepository（MyBatis 视频音频仓储实现）。
 *
 * <p>只在 db Profile（数据库配置环境）下启用，负责保存音频提取结果、ASR（自动语音识别）结果和音频质量信息。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisVideoAudioRepository implements VideoAudioRepository {

    private final VideoAudioMapper videoAudioMapper;

    @Override
    public VideoAudioEntity saveOrUpdateByMaterialId(VideoAudioEntity entity) {
        Optional<VideoAudioEntity> existing = findByMaterialId(entity.getMaterialId());
        if (existing.isEmpty()) {
            videoAudioMapper.insert(entity);
            return entity;
        }
        VideoAudioEntity existingEntity = existing.get();
        entity.setId(existingEntity.getId());
        entity.setAudioId(existingEntity.getAudioId());
        entity.setCreateTime(existingEntity.getCreateTime());
        entity.setUpdateTime(LocalDateTime.now());

        LambdaUpdateWrapper<VideoAudioEntity> updateWrapper = new LambdaUpdateWrapper<VideoAudioEntity>()
                .set(VideoAudioEntity::getAudioFilePath, entity.getAudioFilePath())
                .set(VideoAudioEntity::getDurationSeconds, entity.getDurationSeconds())
                .set(VideoAudioEntity::getHasAudioStream, entity.getHasAudioStream())
                .set(VideoAudioEntity::getAudioExtractStatus, entity.getAudioExtractStatus())
                .set(VideoAudioEntity::getSpeechRecognitionStatus, entity.getSpeechRecognitionStatus())
                .set(VideoAudioEntity::getTranscriptText, entity.getTranscriptText())
                .set(VideoAudioEntity::getSpeechDetected, entity.getSpeechDetected())
                .set(VideoAudioEntity::getSilenceDetected, entity.getSilenceDetected())
                .set(VideoAudioEntity::getAudioQualityMessage, entity.getAudioQualityMessage())
                .set(VideoAudioEntity::getAsrModelName, entity.getAsrModelName())
                .set(VideoAudioEntity::getAsrRawResponse, entity.getAsrRawResponse())
                .set(VideoAudioEntity::getErrorMessage, entity.getErrorMessage())
                .set(VideoAudioEntity::getUpdateTime, entity.getUpdateTime())
                .eq(VideoAudioEntity::getMaterialId, entity.getMaterialId())
                .eq(VideoAudioEntity::getDeleted, 0);
        videoAudioMapper.update(null, updateWrapper);
        return entity;
    }

    @Override
    public Optional<VideoAudioEntity> findByMaterialId(String materialId) {
        LambdaQueryWrapper<VideoAudioEntity> queryWrapper = new LambdaQueryWrapper<VideoAudioEntity>()
                .eq(VideoAudioEntity::getMaterialId, materialId)
                .eq(VideoAudioEntity::getDeleted, 0);
        return Optional.ofNullable(videoAudioMapper.selectOne(queryWrapper));
    }
}

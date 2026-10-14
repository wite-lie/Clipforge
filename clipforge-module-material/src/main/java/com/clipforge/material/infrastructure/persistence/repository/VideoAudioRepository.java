package com.clipforge.material.infrastructure.persistence.repository;

import com.clipforge.material.infrastructure.persistence.entity.VideoAudioEntity;
import java.util.Optional;

/**
 * VideoAudioRepository（视频音频仓储）。
 *
 * <p>该接口负责保存和查询视频音频提取、转写结果。
 */
public interface VideoAudioRepository {

    /**
     * saveOrUpdateByMaterialId（按素材编号保存或更新）用于保证一个视频素材只有一条最新音频记录。
     *
     * @param entity 视频音频实体
     * @return 保存后的实体
     */
    VideoAudioEntity saveOrUpdateByMaterialId(VideoAudioEntity entity);

    /**
     * findByMaterialId（按素材编号查询）用于读取视频音频记录。
     *
     * @param materialId 素材编号
     * @return 可能存在的视频音频记录
     */
    Optional<VideoAudioEntity> findByMaterialId(String materialId);
}

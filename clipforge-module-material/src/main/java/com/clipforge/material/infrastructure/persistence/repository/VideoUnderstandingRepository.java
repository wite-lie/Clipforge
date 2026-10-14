package com.clipforge.material.infrastructure.persistence.repository;

import com.clipforge.material.infrastructure.persistence.entity.VideoUnderstandingEntity;
import java.util.Optional;

/**
 * VideoUnderstandingRepository（视频理解仓储）。
 *
 * <p>该接口负责保存和查询视频理解结果。
 */
public interface VideoUnderstandingRepository {

    /**
     * saveOrUpdateByMaterialId（按素材编号保存或更新）用于保证一个视频素材只有一条最新视频理解记录。
     *
     * @param entity 视频理解实体
     * @return 保存后的实体
     */
    VideoUnderstandingEntity saveOrUpdateByMaterialId(VideoUnderstandingEntity entity);

    /**
     * findByMaterialId（按素材编号查询）用于读取视频理解结果。
     *
     * @param materialId 素材编号
     * @return 可能存在的视频理解实体
     */
    Optional<VideoUnderstandingEntity> findByMaterialId(String materialId);
}

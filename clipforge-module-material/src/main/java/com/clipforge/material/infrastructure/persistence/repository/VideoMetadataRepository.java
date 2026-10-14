package com.clipforge.material.infrastructure.persistence.repository;

import com.clipforge.material.infrastructure.persistence.entity.VideoMetadataEntity;
import java.util.Optional;

/**
 * VideoMetadataRepository（视频元数据仓储）。
 *
 * <p>该接口负责保存和查询视频元数据。
 */
public interface VideoMetadataRepository {

    /**
     * saveOrUpdateByMaterialId（按素材编号保存或更新）用于保证一个视频素材只有一条最新元数据。
     *
     * @param entity 视频元数据实体
     * @return 保存后的实体
     */
    VideoMetadataEntity saveOrUpdateByMaterialId(VideoMetadataEntity entity);

    /**
     * findByMaterialId（按素材编号查询）用于读取视频元数据。
     *
     * @param materialId 素材编号
     * @return 可能存在的视频元数据
     */
    Optional<VideoMetadataEntity> findByMaterialId(String materialId);
}

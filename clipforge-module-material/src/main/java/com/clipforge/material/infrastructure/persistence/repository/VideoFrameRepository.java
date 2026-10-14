package com.clipforge.material.infrastructure.persistence.repository;

import com.clipforge.material.infrastructure.persistence.entity.VideoFrameEntity;
import java.util.List;

/**
 * VideoFrameRepository（视频候选帧仓储）。
 *
 * <p>该接口负责保存和查询视频候选帧。
 */
public interface VideoFrameRepository {

    /**
     * replaceByMaterialId（按素材编号替换）用于逻辑删除旧帧并保存新帧。
     *
     * @param materialId 素材编号
     * @param entities 新候选帧实体列表
     * @return 保存后的候选帧实体列表
     */
    List<VideoFrameEntity> replaceByMaterialId(String materialId, List<VideoFrameEntity> entities);

    /**
     * listByMaterialId（按素材编号查询）用于读取某个视频素材下的候选帧列表。
     *
     * @param materialId 素材编号
     * @return 候选帧实体列表
     */
    List<VideoFrameEntity> listByMaterialId(String materialId);

    /**
     * updateFrameMaterialId（更新帧素材编号）用于把候选帧和注册出来的图片素材关联起来。
     *
     * @param frameId 候选帧编号
     * @param frameMaterialId 帧图片素材编号
     */
    void updateFrameMaterialId(String frameId, String frameMaterialId);

}

package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.VideoSubtitleSegmentEntity;
import java.util.List;

/**
 * VideoSubtitleSegmentRepository（视频字幕片段仓储）。
 *
 * <p>该接口负责保存和查询 ASR（自动语音识别）生成的字幕片段。
 */
public interface VideoSubtitleSegmentRepository {

    /**
     * replaceByMaterialId（按素材编号替换）用于删除旧字幕片段并保存新片段。
     *
     * @param materialId 素材编号
     * @param segments 字幕片段列表
     * @return 保存后的字幕片段列表
     */
    List<VideoSubtitleSegmentEntity> replaceByMaterialId(String materialId, List<VideoSubtitleSegmentEntity> segments);

    /**
     * listByMaterialId（按素材编号查询）用于读取某个视频的字幕片段。
     *
     * @param materialId 素材编号
     * @return 字幕片段列表
     */
    List<VideoSubtitleSegmentEntity> listByMaterialId(String materialId);
}

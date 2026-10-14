package com.clipforge.material.application;

import com.clipforge.contract.material.VideoMetadataResponse;

/**
 * VideoMetadataQueryUseCase（视频元数据查询用例）。
 *
 * <p>该接口读取已经保存的视频元数据，不负责触发 FFprobe（视频探测工具）。
 */
public interface VideoMetadataQueryUseCase {

    /**
     * queryMetadata（查询元数据）读取已经保存的视频元数据。
     */
    VideoMetadataResponse queryMetadata(String materialId);
}

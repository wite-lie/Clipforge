package com.clipforge.material.application;

import com.clipforge.contract.material.VideoFrameResponse;
import java.util.List;

/**
 * VideoFrameQueryUseCase（视频帧查询用例）。
 *
 * <p>该接口读取已经保存的视频候选帧，不触发抽帧或帧素材注册。
 */
public interface VideoFrameQueryUseCase {

    /**
     * listFrames（查询帧列表）读取某个视频素材的候选帧。
     */
    List<VideoFrameResponse> listFrames(String materialId);
}

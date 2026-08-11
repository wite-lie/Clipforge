package com.clipforge.material.sampling;

import com.clipforge.api.dto.VideoFrameResponse;
import java.util.List;

/**
 * MaterialUnderstandingSampler（素材理解抽样器）用于在调用 VLM（视觉语言模型）前筛选需要理解的素材。
 */
public interface MaterialUnderstandingSampler {

    /**
     * selectVideoFrames（选择视频帧）用于从候选帧中选出真正进入图片理解链路的关键帧。
     *
     * @param frames 已注册为图片素材的视频帧列表
     * @return 被选中进入 VLM（视觉语言模型）理解的视频帧列表
     */
    List<VideoFrameResponse> selectVideoFrames(List<VideoFrameResponse> frames);
}

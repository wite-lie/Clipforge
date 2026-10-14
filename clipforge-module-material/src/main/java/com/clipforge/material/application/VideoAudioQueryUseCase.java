package com.clipforge.material.application;

import com.clipforge.contract.material.VideoAudioResponse;
/**
 * VideoAudioQueryUseCase（视频音频查询用例）。
 *
 * <p>该接口读取已经保存的视频音频记录，不触发音频提取或 ASR（自动语音识别）。
 */
public interface VideoAudioQueryUseCase {

    /**
     * queryAudio（查询音频）读取已经保存的视频音频记录。
     */
    VideoAudioResponse queryAudio(String materialId);
}

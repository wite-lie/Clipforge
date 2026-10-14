package com.clipforge.material.application;

import com.clipforge.contract.material.VideoSubtitleSegmentResponse;
import java.util.List;

/**
 * VideoSubtitleQueryUseCase（视频字幕查询用例）。
 *
 * <p>该接口读取已经保存的 ASR（自动语音识别）字幕片段，不触发转写。
 */
public interface VideoSubtitleQueryUseCase {

    /**
     * listSubtitleSegments（查询字幕片段）读取某个视频素材的字幕片段列表。
     */
    List<VideoSubtitleSegmentResponse> listSubtitleSegments(String materialId);
}

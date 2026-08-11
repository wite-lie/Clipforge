package com.clipforge.video.frame;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoFrameCandidate（视频候选帧）。
 *
 * <p>该对象表示一次抽帧计划中的单个时间点。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFrameCandidate {

    /** frameIndex（帧序号）表示候选帧在本次计划中的顺序。 */
    private Integer frameIndex;

    /** timestampSeconds（时间点秒数）表示候选帧在视频中的时间位置。 */
    private Double timestampSeconds;
}

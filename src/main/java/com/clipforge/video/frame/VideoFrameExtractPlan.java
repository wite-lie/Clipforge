package com.clipforge.video.frame;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoFrameExtractPlan（视频帧抽取计划）。
 *
 * <p>该对象保存根据视频时长生成的一组候选帧时间点。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFrameExtractPlan {

    /** materialId（素材编号）表示原视频素材编号。 */
    private String materialId;

    /** durationSeconds（时长秒数）表示原视频总时长。 */
    private Double durationSeconds;

    /** candidates（候选帧列表）表示本次需要抽取的时间点集合。 */
    private List<VideoFrameCandidate> candidates;
}

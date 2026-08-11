package com.clipforge.video.understanding.highlight;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoHighlightRecommendResult（视频高光推荐结果）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoHighlightRecommendResult {

    /** candidates（候选列表）表示按置信度降序排列的高光片段。 */
    private List<VideoHighlightCandidate> candidates;
}

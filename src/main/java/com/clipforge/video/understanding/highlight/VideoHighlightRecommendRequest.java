package com.clipforge.video.understanding.highlight;

import com.clipforge.video.understanding.VideoUnderstandingContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoHighlightRecommendRequest（视频高光推荐请求）。
 *
 * <p>该对象承载高光推荐所需的视频理解事实包。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoHighlightRecommendRequest {

    /** context（视频理解上下文）包含候选帧、字幕、视觉摘要和用户理解指令。 */
    private VideoUnderstandingContext context;
}

package com.clipforge.video.understanding.highlight;

/**
 * VideoHighlightRecommender（视频高光推荐器）。
 *
 * <p>该接口负责把候选帧、字幕、视觉摘要和用户目标综合成结构化高光片段。
 */
public interface VideoHighlightRecommender {

    /**
     * recommend（推荐）用于生成结构化高光候选。
     *
     * @param request 推荐请求
     * @return 推荐结果
     */
    VideoHighlightRecommendResult recommend(VideoHighlightRecommendRequest request);
}

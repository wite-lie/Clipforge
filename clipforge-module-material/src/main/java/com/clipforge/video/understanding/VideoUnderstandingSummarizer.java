package com.clipforge.video.understanding;

import com.clipforge.material.video.understanding.VideoUnderstandingResult;

/**
 * VideoUnderstandingSummarizer（视频理解摘要器）。
 *
 * <p>该接口屏蔽摘要生成策略，当前可通过配置在规则摘要器和 LLMVideoUnderstandingSummarizer（大模型视频理解摘要器）之间切换。
 */
public interface VideoUnderstandingSummarizer {

    /**
     * summarize（生成摘要）用于把视频理解上下文转成结构化视频理解结果。
     *
     * @param context 视频理解上下文
     * @return 视频理解结果
     */
    VideoUnderstandingResult summarize(VideoUnderstandingContext context);
}

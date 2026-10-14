package com.clipforge.video.understanding;

import com.clipforge.material.infrastructure.persistence.entity.VideoMetadataEntity;
import com.clipforge.material.MaterialUnderstandingResult;
import com.clipforge.video.understanding.highlight.VideoSubtitleSegmentContext;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoUnderstandingContext（视频理解上下文）。
 *
 * <p>该对象是视频理解局部 Agent（智能体）的证据包，包含元数据、关键帧理解结果、音频转写和非致命警告。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUnderstandingContext {

    /** runId（运行编号）表示本次视频理解所属的执行链路。 */
    private String runId;

    /** materialId（素材编号）表示被理解的视频素材编号。 */
    private String materialId;

    /** instruction（理解指令）表示调用方希望从什么创作角度理解视频。 */
    private String instruction;

    /** metadata（视频元数据）表示 FFprobe（视频探测工具）读取出的媒体信息。 */
    private VideoMetadataEntity metadata;

    /** frames（候选帧上下文列表）表示参与理解的关键画面。 */
    private List<VideoUnderstandingFrameContext> frames;

    /** materialUnderstandingResult（素材理解结果）表示候选帧图片理解和摘要结果。 */
    private MaterialUnderstandingResult materialUnderstandingResult;

    /** audioContext（音频上下文）表示音频提取与转写结果。 */
    private VideoUnderstandingAudioContext audioContext;

    /** subtitleSegments（字幕片段列表）表示 ASR（自动语音识别）生成的带时间戳字幕。 */
    private List<VideoSubtitleSegmentContext> subtitleSegments;

    /** warnings（警告列表）表示可以降级处理但需要复盘的问题。 */
    private List<String> warnings;
}

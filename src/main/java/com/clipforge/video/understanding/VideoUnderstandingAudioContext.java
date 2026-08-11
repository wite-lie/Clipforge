package com.clipforge.video.understanding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoUnderstandingAudioContext（视频理解音频上下文）。
 *
 * <p>该对象承接音频提取和 ASR（自动语音识别）转写结果，供视频理解摘要器判断音频线索是否可用。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUnderstandingAudioContext {

    /** audioId（音频编号）表示视频音频业务编号。 */
    private String audioId;

    /** hasAudioStream（是否有音轨）表示视频是否存在音频流。 */
    private Boolean hasAudioStream;

    /** audioExtractStatus（音频提取状态）表示 FFmpeg（音视频处理工具）提取结果。 */
    private String audioExtractStatus;

    /** speechRecognitionStatus（语音识别状态）表示 ASR（自动语音识别）转写结果。 */
    private String speechRecognitionStatus;

    /** transcriptText（转写文本）表示可供创作参考的音频文本。 */
    private String transcriptText;

    /** errorMessage（错误消息）表示音频处理失败或跳过原因。 */
    private String errorMessage;
}

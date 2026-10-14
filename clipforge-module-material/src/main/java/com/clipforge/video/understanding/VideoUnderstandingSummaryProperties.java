package com.clipforge.video.understanding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VideoUnderstandingSummaryProperties（视频理解摘要配置）。
 *
 * <p>该配置控制视频级摘要使用规则摘要器还是 LLM（大语言模型）摘要器，以及上下文裁剪和调用阈值。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.video.understanding")
public class VideoUnderstandingSummaryProperties {

    /** summarizerMode（摘要模式）支持 rule、llm、llm-strict。 */
    private String summarizerMode = "rule";

    /** minUsableFrameCount（最少可用帧数量）表示调用 LLM 摘要前至少需要多少个候选帧。 */
    private Integer minUsableFrameCount = 2;

    /** minAudioTextLength（最少音频文本长度）表示调用 LLM 摘要前 ASR（自动语音识别）文本至少多少字符。 */
    private Integer minAudioTextLength = 20;

    /** maxFrameSummaryChars（帧摘要最大字符数）用于防止图片理解摘要过长。 */
    private Integer maxFrameSummaryChars = 4000;

    /** maxAudioTranscriptChars（音频转写最大字符数）用于防止 ASR 文本过长。 */
    private Integer maxAudioTranscriptChars = 6000;

    /** maxOutputTokens（最大输出词元）表示 LLM 视频摘要最多生成多少 Token（词元）。 */
    private Integer maxOutputTokens = 1600;

    /** temperature（温度）越低越稳定，视频摘要建议保持较低随机性。 */
    private Double temperature = 0.2;

    /** promptVersion（提示词版本）用于落库复盘。 */
    private String promptVersion = "video-understanding-summary-v1";

    /** systemPath（系统提示词路径）定义视频摘要模型角色、边界和输出格式。 */
    private String systemPath = "classpath:prompts/视频理解摘要-系统提示词.md";

    /** userPath（用户提示词路径）定义视频理解事实包和用户指令的输入格式。 */
    private String userPath = "classpath:prompts/视频理解摘要-用户提示词.md";

    /** logPrompt（是否打印提示词）仅用于本地调试，默认关闭。 */
    private boolean logPrompt = false;

    /** logResponse（是否打印响应）仅用于本地调试，默认关闭。 */
    private boolean logResponse = false;

    public VideoUnderstandingSummarizerMode mode() {
        return VideoUnderstandingSummarizerMode.fromConfig(summarizerMode);
    }
}

package com.clipforge.video.ffmpeg;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FFmpegProperties（音视频处理配置属性）。
 *
 * <p>集中管理 FFprobe（视频探测工具）、FFmpeg（音视频处理工具）、抽帧、音频提取和静音检测参数。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.video.ffmpeg")
public class FFmpegProperties {

    /** FFprobe（视频探测工具）可执行文件路径。 */
    private String ffprobePath = "ffprobe";

    /** FFmpeg（音视频处理工具）可执行文件路径。 */
    private String ffmpegPath = "ffmpeg";

    /** 视频元数据探测超时秒数。 */
    private long probeTimeoutSeconds = 15;

    /** 抽帧超时秒数。 */
    private long frameExtractTimeoutSeconds = 20;

    /** 音频提取超时秒数。 */
    private long audioExtractTimeoutSeconds = 30;

    /** 静音检测超时秒数。 */
    private long silenceDetectTimeoutSeconds = 30;

    /** 静音噪声阈值，低于该分贝值的片段会被视作静音候选。 */
    private int silenceNoiseDb = -35;

    /** 连续静音达到该时长才记录为静音段。 */
    private double silenceMinDurationSeconds = 0.5;

    /** 静音占比达到该值时认为音频近似静音。 */
    private double silenceRatioThreshold = 0.9;

    /** 视频理解阶段建议接受的最短视频时长。 */
    private double minDurationSeconds = 1;

    /** 视频理解阶段建议接受的最长视频时长，默认 3 分钟。 */
    private double maxDurationSeconds = 180;
}

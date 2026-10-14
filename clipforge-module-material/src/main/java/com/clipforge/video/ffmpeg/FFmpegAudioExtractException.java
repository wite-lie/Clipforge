package com.clipforge.video.ffmpeg;

import com.clipforge.video.audio.AudioExtractStatus;
import lombok.Getter;

/**
 * FFmpegAudioExtractException（FFmpeg 音频提取异常）。
 *
 * <p>该异常用于封装 FFmpeg（音视频处理工具）提取音频失败、超时或不可用的场景。
 */
@Getter
public class FFmpegAudioExtractException extends RuntimeException {

    /** extractStatus（提取状态）表示失败对应的业务状态。 */
    private final AudioExtractStatus extractStatus;

    /**
     * 构造函数用于创建音频提取异常。
     *
     * @param extractStatus 提取状态
     * @param message 异常消息
     */
    public FFmpegAudioExtractException(AudioExtractStatus extractStatus, String message) {
        // 调用父类保存异常消息。
        super(message);
        // 保存提取状态。
        this.extractStatus = extractStatus;
    }

    /**
     * 构造函数用于创建带原始异常的音频提取异常。
     *
     * @param extractStatus 提取状态
     * @param message 异常消息
     * @param cause 原始异常
     */
    public FFmpegAudioExtractException(AudioExtractStatus extractStatus, String message, Throwable cause) {
        // 调用父类保存异常消息和原始异常。
        super(message, cause);
        // 保存提取状态。
        this.extractStatus = extractStatus;
    }
}

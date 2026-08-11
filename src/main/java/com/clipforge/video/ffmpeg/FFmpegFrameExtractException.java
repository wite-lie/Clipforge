package com.clipforge.video.ffmpeg;

import com.clipforge.video.frame.VideoFrameExtractStatus;
import lombok.Getter;

/**
 * FFmpegFrameExtractException（FFmpeg 帧抽取异常）。
 *
 * <p>该异常用于封装 FFmpeg（音视频处理工具）抽取候选帧失败、超时或不可用的场景。
 */
@Getter
public class FFmpegFrameExtractException extends RuntimeException {

    /** extractStatus（抽取状态）表示失败对应的业务状态。 */
    private final VideoFrameExtractStatus extractStatus;

    /**
     * 构造函数用于创建帧抽取异常。
     *
     * @param extractStatus 抽取状态
     * @param message 异常消息
     */
    public FFmpegFrameExtractException(VideoFrameExtractStatus extractStatus, String message) {
        // 调用父类保存异常消息。
        super(message);
        // 保存抽取状态。
        this.extractStatus = extractStatus;
    }

    /**
     * 构造函数用于创建带原始异常的帧抽取异常。
     *
     * @param extractStatus 抽取状态
     * @param message 异常消息
     * @param cause 原始异常
     */
    public FFmpegFrameExtractException(VideoFrameExtractStatus extractStatus, String message, Throwable cause) {
        // 调用父类保存异常消息和原始异常。
        super(message, cause);
        // 保存抽取状态。
        this.extractStatus = extractStatus;
    }
}

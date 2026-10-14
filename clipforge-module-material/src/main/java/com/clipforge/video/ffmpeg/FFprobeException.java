package com.clipforge.video.ffmpeg;

import com.clipforge.video.metadata.VideoMetadataProbeStatus;
import lombok.Getter;

/**
 * FFprobeException（FFprobe 异常）。
 *
 * <p>该异常用于封装 FFprobe（视频探测工具）执行失败、超时或返回异常退出码的场景。
 */
@Getter
public class FFprobeException extends RuntimeException {

    /** probeStatus（探测状态）表示失败对应的业务状态。 */
    private final VideoMetadataProbeStatus probeStatus;

    /**
     * 构造函数用于创建 FFprobe 异常。
     *
     * @param probeStatus 探测状态
     * @param message 异常消息
     */
    public FFprobeException(VideoMetadataProbeStatus probeStatus, String message) {
        // 调用父类保存异常消息。
        super(message);
        // 保存探测状态。
        this.probeStatus = probeStatus;
    }

    /**
     * 构造函数用于创建带原始异常的 FFprobe 异常。
     *
     * @param probeStatus 探测状态
     * @param message 异常消息
     * @param cause 原始异常
     */
    public FFprobeException(VideoMetadataProbeStatus probeStatus, String message, Throwable cause) {
        // 调用父类保存异常消息和原始异常。
        super(message, cause);
        // 保存探测状态。
        this.probeStatus = probeStatus;
    }
}

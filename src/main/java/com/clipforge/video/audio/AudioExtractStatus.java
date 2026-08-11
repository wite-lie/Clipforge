package com.clipforge.video.audio;

/**
 * AudioExtractStatus（音频提取状态）。
 *
 * <p>该枚举用于描述 FFmpeg（音视频处理工具）从视频中提取音频的结果。
 */
public enum AudioExtractStatus {

    /** PENDING（待处理）表示尚未执行音频提取。 */
    PENDING,

    /** SUCCEEDED（提取成功）表示已经生成音频文件。 */
    SUCCEEDED,

    /** NO_AUDIO_STREAM（无音轨）表示视频元数据中没有音频流。 */
    NO_AUDIO_STREAM,

    /** VIDEO_FILE_NOT_FOUND（视频文件不存在）表示原视频文件路径不存在。 */
    VIDEO_FILE_NOT_FOUND,

    /** FFMPEG_UNAVAILABLE（FFmpeg 不可用）表示 FFmpeg 命令无法启动或不可用。 */
    FFMPEG_UNAVAILABLE,

    /** TIMEOUT（超时）表示 FFmpeg 命令执行超过配置时间。 */
    TIMEOUT,

    /** FAILED（失败）表示音频提取发生普通失败。 */
    FAILED;

    /**
     * value（数据库值）用于返回写入数据库的状态字符串。
     *
     * @return 数据库状态值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}

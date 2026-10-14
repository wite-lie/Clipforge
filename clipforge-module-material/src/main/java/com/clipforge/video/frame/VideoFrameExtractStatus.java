package com.clipforge.video.frame;

/**
 * VideoFrameExtractStatus（视频帧抽取状态）。
 *
 * <p>该枚举用于描述 FFmpeg（音视频处理工具）抽取候选帧的结果。
 */
public enum VideoFrameExtractStatus {

    /** PENDING（待抽取）表示候选帧还未执行抽取。 */
    PENDING,

    /** SUCCEEDED（抽取成功）表示候选帧图片已经生成。 */
    SUCCEEDED,

    /** FAILED（抽取失败）表示抽帧过程发生普通失败。 */
    FAILED,

    /** VIDEO_METADATA_MISSING（缺少视频元数据）表示尚未成功读取视频元数据。 */
    VIDEO_METADATA_MISSING,

    /** VIDEO_FILE_NOT_FOUND（视频文件不存在）表示原视频文件路径不存在。 */
    VIDEO_FILE_NOT_FOUND,

    /** FFMPEG_UNAVAILABLE（FFmpeg 不可用）表示 FFmpeg 命令无法启动或不可用。 */
    FFMPEG_UNAVAILABLE,

    /** TIMEOUT（抽取超时）表示 FFmpeg 命令执行超过配置时间。 */
    TIMEOUT;

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

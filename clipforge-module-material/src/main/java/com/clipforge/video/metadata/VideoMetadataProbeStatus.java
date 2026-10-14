package com.clipforge.video.metadata;

/**
 * VideoMetadataProbeStatus（视频元数据探测状态）。
 *
 * <p>该枚举用于描述 FFprobe（视频探测工具）读取视频元数据的结果。
 */
public enum VideoMetadataProbeStatus {

    /** PENDING（待读取）表示视频元数据尚未读取。 */
    PENDING,

    /** SUCCEEDED（读取成功）表示视频元数据已经成功读取。 */
    SUCCEEDED,

    /** FAILED（读取失败）表示读取过程发生普通失败。 */
    FAILED,

    /** FILE_NOT_FOUND（文件不存在）表示素材表中的本地文件路径不存在。 */
    FILE_NOT_FOUND,

    /** UNSUPPORTED_FORMAT（格式不支持）表示 FFprobe 无法识别当前视频格式。 */
    UNSUPPORTED_FORMAT,

    /** TIMEOUT（读取超时）表示 FFprobe 命令执行超过配置时间。 */
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

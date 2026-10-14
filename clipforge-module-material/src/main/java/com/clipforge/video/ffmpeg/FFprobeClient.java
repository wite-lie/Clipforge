package com.clipforge.video.ffmpeg;

import java.nio.file.Path;

/**
 * FFprobeClient（视频探测客户端）。
 *
 * <p>该接口定义读取视频原始媒体信息的能力，业务层不直接拼接命令。
 */
public interface FFprobeClient {

    /**
     * probe（探测）用于调用 FFprobe（视频探测工具）读取视频元数据 JSON（对象表示格式）。
     *
     * @param videoPath 视频文件路径
     * @return FFprobe 探测结果
     */
    FFprobeResult probe(Path videoPath);
}

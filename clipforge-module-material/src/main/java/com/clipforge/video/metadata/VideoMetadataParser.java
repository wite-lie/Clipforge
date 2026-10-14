package com.clipforge.video.metadata;

/**
 * VideoMetadataParser（视频元数据解析器）。
 *
 * <p>该接口负责把 FFprobe（视频探测工具）原始 JSON（对象表示格式）解析为结构化视频元数据。
 */
public interface VideoMetadataParser {

    /**
     * parse（解析）用于解析 FFprobe 原始 JSON。
     *
     * @param rawJson FFprobe 原始 JSON
     * @return 视频元数据
     */
    VideoMetadata parse(String rawJson);
}

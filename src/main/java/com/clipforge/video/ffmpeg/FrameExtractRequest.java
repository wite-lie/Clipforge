package com.clipforge.video.ffmpeg;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FrameExtractRequest（帧抽取请求）。
 *
 * <p>该对象描述 FFmpeg（音视频处理工具）抽取单张候选帧需要的参数。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrameExtractRequest {

    /** videoPath（视频路径）表示原视频文件路径。 */
    private Path videoPath;

    /** timestampSeconds（时间点秒数）表示从视频哪个时间点抽帧。 */
    private Double timestampSeconds;

    /** outputPath（输出路径）表示候选帧图片保存路径。 */
    private Path outputPath;

    /** jpegQuality（JPEG 质量）表示 FFmpeg -q:v 参数。 */
    private Integer jpegQuality;
}

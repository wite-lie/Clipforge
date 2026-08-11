package com.clipforge.video.ffmpeg;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AudioExtractRequest（音频提取请求）。
 *
 * <p>该对象描述 FFmpeg（音视频处理工具）从视频中提取音频需要的参数。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioExtractRequest {

    /** videoPath（视频路径）表示原视频文件路径。 */
    private Path videoPath;

    /** outputPath（输出路径）表示音频文件保存路径。 */
    private Path outputPath;

    /** sampleRate（采样率）表示输出音频采样率。 */
    private Integer sampleRate;

    /** channels（声道数）表示输出音频声道数。 */
    private Integer channels;
}

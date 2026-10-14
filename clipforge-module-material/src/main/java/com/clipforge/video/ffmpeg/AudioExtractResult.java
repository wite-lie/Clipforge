package com.clipforge.video.ffmpeg;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AudioExtractResult（音频提取结果）。
 *
 * <p>该对象保存 FFmpeg（音视频处理工具）提取音频后的结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioExtractResult {

    /** outputPath（输出路径）表示音频文件保存路径。 */
    private Path outputPath;

    /** exitCode（退出码）表示 FFmpeg 进程执行结果。 */
    private int exitCode;
}

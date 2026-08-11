package com.clipforge.video.ffmpeg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * FFprobeResult（FFprobe 探测结果）。
 *
 * <p>该对象保存 FFprobe（视频探测工具）命令执行后的原始输出。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FFprobeResult {

    /** rawJson（原始 JSON）表示 FFprobe 标准输出内容。 */
    private String rawJson;

    /** exitCode（退出码）表示进程执行结果。 */
    private int exitCode;
}

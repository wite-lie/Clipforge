package com.clipforge.video.audio;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AudioSilenceDetectRequest（音频静音检测请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioSilenceDetectRequest {

    /** 音频文件路径。 */
    private Path audioPath;

    /** 音频总时长，单位秒。 */
    private Double durationSeconds;
}

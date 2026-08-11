package com.clipforge.video.audio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AudioSilenceDetectResult（音频静音检测结果）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioSilenceDetectResult {

    /** 是否近似静音。 */
    private boolean silenceDetected;

    /** 静音时长占比。 */
    private double silenceRatio;

    /** 静音总时长，单位秒。 */
    private double silenceSeconds;

    /** 检测说明。 */
    private String message;
}

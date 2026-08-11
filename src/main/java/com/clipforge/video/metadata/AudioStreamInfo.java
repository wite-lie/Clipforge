package com.clipforge.video.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AudioStreamInfo（音频流信息）。
 *
 * <p>该对象保存 FFprobe（视频探测工具）返回的第一条音频流关键字段。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioStreamInfo {

    /** codecName（编码名称）表示音频编码格式，例如 aac。 */
    private String codecName;
}

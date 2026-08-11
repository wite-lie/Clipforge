package com.clipforge.video.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoStreamInfo（视频流信息）。
 *
 * <p>该对象保存 FFprobe（视频探测工具）返回的第一条视频流关键字段。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStreamInfo {

    /** codecName（编码名称）表示视频编码格式，例如 h264。 */
    private String codecName;

    /** width（宽度）表示视频画面宽度，单位像素。 */
    private Integer width;

    /** height（高度）表示视频画面高度，单位像素。 */
    private Integer height;

    /** frameRate（帧率）表示视频每秒帧数。 */
    private Double frameRate;
}

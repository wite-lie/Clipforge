package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoMetadataResponse（视频元数据响应）。
 *
 * <p>该对象用于视频元数据读取和查询接口返回结构化媒体信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadataResponse {

    /** metadataId（元数据编号）表示视频元数据业务编号。 */
    private String metadataId;

    /** materialId（素材编号）表示视频素材编号。 */
    private String materialId;

    /** durationSeconds（时长秒数）表示视频总时长。 */
    private Double durationSeconds;

    /** width（宽度）表示视频宽度，单位像素。 */
    private Integer width;

    /** height（高度）表示视频高度，单位像素。 */
    private Integer height;

    /** frameRate（帧率）表示视频每秒帧数。 */
    private Double frameRate;

    /** bitRate（码率）表示整体码率，单位 bit/s。 */
    private Long bitRate;

    /** formatName（格式名称）表示视频容器格式。 */
    private String formatName;

    /** videoCodec（视频编码）表示视频流编码格式。 */
    private String videoCodec;

    /** hasAudio（是否有音轨）表示视频是否存在音频流。 */
    private Boolean hasAudio;

    /** audioCodec（音频编码）表示第一条音轨编码格式。 */
    private String audioCodec;

    /** audioStreamCount（音轨数量）表示音频流数量。 */
    private Integer audioStreamCount;

    /** probeStatus（探测状态）表示 FFprobe（视频探测工具）读取结果。 */
    private String probeStatus;

    /** probeErrorMessage（探测错误消息）表示读取失败原因。 */
    private String probeErrorMessage;

    /** durationWithinConfiguredRange（时长是否在配置范围内）表示视频是否符合当前建议时长。 */
    private Boolean durationWithinConfiguredRange;

    /** durationWarningMessage（时长提示消息）表示视频过短或过长时的提醒。 */
    private String durationWarningMessage;
}

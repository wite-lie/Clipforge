package com.clipforge.video.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoMetadata（视频元数据）。
 *
 * <p>该对象是视频真实媒体信息的结构化表示，供持久化、接口响应和后续抽帧策略复用。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {

    /** durationSeconds（时长秒数）表示视频总时长。 */
    private Double durationSeconds;

    /** bitRate（码率）表示整体码率，单位 bit/s。 */
    private Long bitRate;

    /** formatName（格式名称）表示容器格式。 */
    private String formatName;

    /** videoStream（视频流）表示第一条视频流信息。 */
    private VideoStreamInfo videoStream;

    /** audioStream（音频流）表示第一条音频流信息。 */
    private AudioStreamInfo audioStream;

    /** audioStreamCount（音轨数量）表示视频文件中的音频流数量。 */
    private Integer audioStreamCount;

    /** rawProbeJson（原始探测 JSON）表示 FFprobe 返回的完整 JSON 文本。 */
    private String rawProbeJson;

    /**
     * hasAudio（是否有音频）用于判断视频是否包含至少一条音轨。
     *
     * @return 存在音轨时返回 true（真）
     */
    public boolean hasAudio() {
        // 音轨数量大于 0 视为存在音频。
        return audioStreamCount != null && audioStreamCount > 0;
    }
}

package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoAudioResponse（视频音频响应）。
 *
 * <p>该对象用于音频提取、音频查询和 ASR（自动语音识别）转写接口返回音频处理结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAudioResponse {

    /** audioId（音频编号）表示视频音频业务编号。 */
    private String audioId;

    /** materialId（素材编号）表示原视频素材编号。 */
    private String materialId;

    /** audioFilePath（音频文件路径）表示提取出的音频文件保存位置。 */
    private String audioFilePath;

    /** durationSeconds（时长秒数）表示音频时长，第一版直接复用视频时长。 */
    private Double durationSeconds;

    /** hasAudioStream（是否有音轨）表示视频是否存在音频流。 */
    private Boolean hasAudioStream;

    /** audioExtractStatus（音频提取状态）表示 FFmpeg（音视频处理工具）提取结果。 */
    private String audioExtractStatus;

    /** speechRecognitionStatus（语音识别状态）表示 ASR（自动语音识别）转写结果。 */
    private String speechRecognitionStatus;

    /** transcriptText（转写文本）表示 ASR（自动语音识别）生成的全文文本。 */
    private String transcriptText;

    /** speechDetected（是否检测到人声）表示音频中是否有可用人声。 */
    private Boolean speechDetected;

    /** silenceDetected（是否检测到静音）表示音频是否静音或近似静音。 */
    private Boolean silenceDetected;

    /** audioQualityMessage（音频质量说明）用于前端展示静音、无人声或可识别等状态。 */
    private String audioQualityMessage;

    /** asrModelName（ASR 模型名称）表示实际调用的语音识别模型。 */
    private String asrModelName;

    /** subtitleSegmentCount（字幕片段数量）表示当前保存了多少条带时间戳字幕。 */
    private Integer subtitleSegmentCount;

    /** errorMessage（错误消息）表示失败或跳过原因。 */
    private String errorMessage;
}

package com.clipforge.material.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoAudioEntity（视频音频实体）。
 *
 * <p>该实体映射 cf_video_audio（视频音频表），用于保存音频提取结果和 ASR（自动语音识别）转写结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_video_audio")
public class VideoAudioEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** audioId（音频编号）是视频音频业务编号。 */
    private String audioId;

    /** materialId（素材编号）表示对应的视频素材编号。 */
    private String materialId;

    /** audioFilePath（音频文件路径）表示提取出的音频文件保存位置。 */
    private String audioFilePath;

    /** durationSeconds（时长秒数）表示音频时长，第一版直接复用视频时长。 */
    private Double durationSeconds;

    /** hasAudioStream（是否有音轨）0 表示没有，1 表示有。 */
    private Integer hasAudioStream;

    /** audioExtractStatus（音频提取状态）表示 FFmpeg（音视频处理工具）提取结果。 */
    private String audioExtractStatus;

    /** speechRecognitionStatus（语音识别状态）表示 ASR（自动语音识别）转写结果。 */
    private String speechRecognitionStatus;

    /** transcriptText（转写文本）表示 ASR（自动语音识别）生成的全文文本。 */
    private String transcriptText;

    /** speechDetected（是否检测到人声）1 表示有可用人声，0 表示未检测到。 */
    private Integer speechDetected;

    /** silenceDetected（是否检测到静音）1 表示静音或近似静音，0 表示非静音。 */
    private Integer silenceDetected;

    /** audioQualityMessage（音频质量说明）描述静音、无人声或可识别等情况。 */
    private String audioQualityMessage;

    /** asrModelName（ASR 模型名称）表示实际调用的自动语音识别模型。 */
    private String asrModelName;

    /** asrRawResponse（ASR 原始响应）用于调试供应商返回内容。 */
    private String asrRawResponse;

    /** errorMessage（错误消息）表示失败或跳过原因。 */
    private String errorMessage;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}

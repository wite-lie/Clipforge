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
 * VideoMetadataEntity（视频元数据实体）。
 *
 * <p>该实体映射 cf_video_metadata（视频元数据表），用于保存 FFprobe（视频探测工具）读取到的媒体信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_video_metadata")
public class VideoMetadataEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** metadataId（元数据编号）是视频元数据业务编号。 */
    private String metadataId;

    /** materialId（素材编号）表示对应的视频素材编号。 */
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

    /** formatName（格式名称）表示容器格式名称。 */
    private String formatName;

    /** videoCodec（视频编码）表示视频流编码格式。 */
    private String videoCodec;

    /** hasAudio（是否有音轨）0 表示没有，1 表示有。 */
    private Integer hasAudio;

    /** audioCodec（音频编码）表示第一条音轨编码格式。 */
    private String audioCodec;

    /** audioStreamCount（音轨数量）表示音频流数量。 */
    private Integer audioStreamCount;

    /** probeStatus（探测状态）表示 FFprobe 读取结果。 */
    private String probeStatus;

    /** probeErrorMessage（探测错误消息）表示失败原因。 */
    private String probeErrorMessage;

    /** rawProbeJson（原始探测 JSON）表示 FFprobe 原始输出。 */
    private String rawProbeJson;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}

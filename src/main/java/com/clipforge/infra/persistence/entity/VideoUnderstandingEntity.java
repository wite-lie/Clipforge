package com.clipforge.infra.persistence.entity;

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
 * VideoUnderstandingEntity（视频理解实体）。
 *
 * <p>该实体映射 cf_video_understanding（视频理解结果表），用于保存视频级结构化理解结果和关键证据快照。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_video_understanding")
public class VideoUnderstandingEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** understandingId（理解编号）是视频理解业务编号。 */
    private String understandingId;

    /** materialId（素材编号）表示对应的视频素材编号。 */
    private String materialId;

    /** videoMetadataSnapshot（视频元数据快照）表示生成理解结果时使用的元数据 JSON。 */
    private String videoMetadataSnapshot;

    /** frameSummarySnapshot（候选帧摘要快照）表示生成理解结果时使用的图片理解摘要 JSON。 */
    private String frameSummarySnapshot;

    /** audioTranscriptSnapshot（音频转写快照）表示生成理解结果时使用的 ASR（自动语音识别）文本。 */
    private String audioTranscriptSnapshot;

    /** understandingJson（理解结果 JSON）表示结构化 VideoUnderstandingResult（视频理解结果）。 */
    private String understandingJson;

    /** understandingStatus（理解状态）表示视频理解是否完整成功。 */
    private String understandingStatus;

    /** errorMessage（错误消息）表示失败或部分成功原因。 */
    private String errorMessage;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}

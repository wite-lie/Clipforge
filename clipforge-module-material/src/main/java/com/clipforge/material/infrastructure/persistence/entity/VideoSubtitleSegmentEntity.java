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
 * VideoSubtitleSegmentEntity（视频字幕片段实体）。
 *
 * <p>该实体映射 cf_video_subtitle_segment（视频字幕片段表），用于保存 ASR（自动语音识别）返回的时间戳字幕。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_video_subtitle_segment")
public class VideoSubtitleSegmentEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** segmentId（片段编号）是字幕片段业务编号。 */
    private String segmentId;

    /** audioId（音频编号）表示字幕所属音频记录。 */
    private String audioId;

    /** materialId（素材编号）表示字幕所属视频素材。 */
    private String materialId;

    /** segmentIndex（片段序号）表示字幕顺序。 */
    private Integer segmentIndex;

    /** startSeconds（开始秒数）表示字幕片段开始时间。 */
    private Double startSeconds;

    /** endSeconds（结束秒数）表示字幕片段结束时间。 */
    private Double endSeconds;

    /** text（文本）表示字幕内容。 */
    private String text;

    /** confidence（置信度）表示识别置信度。 */
    private Double confidence;

    /** source（来源）表示 ASR/MOCK/MANUAL。 */
    private String source;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}

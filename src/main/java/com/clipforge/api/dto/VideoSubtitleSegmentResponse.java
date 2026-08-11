package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoSubtitleSegmentResponse（视频字幕片段响应）。
 *
 * <p>该对象用于返回 ASR（自动语音识别）生成的带时间戳字幕片段。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubtitleSegmentResponse {

    /** segmentId（片段编号）表示字幕片段业务编号。 */
    private String segmentId;

    /** audioId（音频编号）表示字幕所属视频音频记录。 */
    private String audioId;

    /** materialId（素材编号）表示字幕所属视频素材。 */
    private String materialId;

    /** segmentIndex（片段序号）表示字幕在整段音频中的顺序。 */
    private Integer segmentIndex;

    /** startSeconds（开始秒数）表示字幕开始时间。 */
    private Double startSeconds;

    /** endSeconds（结束秒数）表示字幕结束时间。 */
    private Double endSeconds;

    /** text（文本）表示字幕内容。 */
    private String text;

    /** confidence（置信度）表示 ASR 供应商返回的识别置信度。 */
    private Double confidence;

    /** source（来源）表示 ASR（自动语音识别）、MOCK（模拟）或 MANUAL（人工）。 */
    private String source;
}

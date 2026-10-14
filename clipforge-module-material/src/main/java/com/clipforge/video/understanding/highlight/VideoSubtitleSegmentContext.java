package com.clipforge.video.understanding.highlight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoSubtitleSegmentContext（视频字幕片段上下文）。
 *
 * <p>该对象是视频理解上下文中的字幕时间线片段，来源于 ASR（自动语音识别）字幕表。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSubtitleSegmentContext {

    /** segmentId（片段编号）表示字幕片段业务编号。 */
    private String segmentId;

    /** segmentIndex（片段序号）表示字幕顺序。 */
    private Integer segmentIndex;

    /** startSeconds（开始秒数）表示字幕开始时间。 */
    private Double startSeconds;

    /** endSeconds（结束秒数）表示字幕结束时间。 */
    private Double endSeconds;

    /** text（文本）表示字幕内容。 */
    private String text;

    /** confidence（置信度）表示 ASR（自动语音识别）识别置信度。 */
    private Double confidence;
}

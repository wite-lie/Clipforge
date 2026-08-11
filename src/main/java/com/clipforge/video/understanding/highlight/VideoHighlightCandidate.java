package com.clipforge.video.understanding.highlight;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoHighlightCandidate（视频高光候选片段）。
 *
 * <p>该对象用结构化方式描述一个可能适合创作引用或后续剪辑的片段。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoHighlightCandidate {

    /** startSeconds（开始秒数）表示候选片段开始时间。 */
    private Double startSeconds;

    /** endSeconds（结束秒数）表示候选片段结束时间。 */
    private Double endSeconds;

    /** reason（推荐原因）说明该片段为什么适合作为高光。 */
    private String reason;

    /** confidence（置信度）表示综合规则评分，范围 0 到 1。 */
    private Double confidence;

    /** evidenceTypes（证据类型）表示本片段命中的画面、字幕、关键词等依据。 */
    private List<String> evidenceTypes;

    /** keywords（关键词）表示本片段命中的主题词、字幕词或画面主体词。 */
    private List<String> keywords;
}

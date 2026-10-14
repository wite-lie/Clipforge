package com.clipforge.ai.api.speech;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SpeechRecognitionSegment（语音识别片段）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionSegment {

    /** 片段开始时间，单位秒。 */
    private Double startSeconds;

    /** 片段结束时间，单位秒。 */
    private Double endSeconds;

    /** 片段文本。 */
    private String text;

    /** 识别置信度。 */
    private Double confidence;
}

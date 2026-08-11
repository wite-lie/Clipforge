package com.clipforge.model.speech;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SpeechRecognitionModelResponse（语音识别模型响应）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionModelResponse {

    /** 模型供应商。 */
    private String provider;

    /** ASR（自动语音识别）模型名称。 */
    private String modelName;

    /** 全文转写文本。 */
    private String transcriptText;

    /** 是否识别到可用人声。 */
    private Boolean speechDetected;

    /** 带时间戳的字幕片段。 */
    private List<SpeechRecognitionSegment> segments;

    /** 供应商原始响应或 Mock（模拟）说明。 */
    private String rawContent;
}

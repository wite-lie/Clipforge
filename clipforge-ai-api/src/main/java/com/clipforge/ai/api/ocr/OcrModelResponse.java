package com.clipforge.ai.api.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OcrModelResponse（OCR 模型响应）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrModelResponse {

    /** provider（供应商）表示实际调用的 OCR 引擎来源。 */
    private String provider;

    /** modelName（模型名称）表示实际使用的 OCR 模型或引擎。 */
    private String modelName;

    /** text（识别文本）表示 OCR 输出的正文。 */
    private String text;

    /** confidence（置信度）表示供应商提供的整体识别置信度，可为空。 */
    private Double confidence;

    /** warnings（警告）表示识别过程中的降级、低清晰度或部分识别提示。 */
    private String warnings;
}

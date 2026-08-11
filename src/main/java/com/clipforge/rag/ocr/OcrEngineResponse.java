package com.clipforge.rag.ocr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OcrEngineResponse（OCR 识别响应）。
 *
 * <p>该对象保存 OCR（光学字符识别）返回的文本和一些可选提示信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrEngineResponse {

    /** text（文本）表示 OCR 提取出的正文。 */
    private String text;

    /** engineName（引擎名称）表示本次实际使用的 OCR 供应商或实现名。 */
    private String engineName;

    /** warnings（警告）表示识别过程中的提示信息。 */
    private String warnings;
}

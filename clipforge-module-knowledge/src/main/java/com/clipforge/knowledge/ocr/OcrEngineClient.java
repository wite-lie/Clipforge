package com.clipforge.knowledge.ocr;

/**
 * OcrEngineClient（OCR 引擎客户端）。
 *
 * <p>该接口用于屏蔽不同 OCR（光学字符识别）供应商的调用差异，后续可以替换为真实引擎实现。
 */
public interface OcrEngineClient {

    /**
     * recognize（识别）用于从扫描件或图片中提取文本。
     *
     * @param request OCR 识别请求
     * @return OCR 识别结果
     */
    OcrEngineResponse recognize(OcrEngineRequest request);
}

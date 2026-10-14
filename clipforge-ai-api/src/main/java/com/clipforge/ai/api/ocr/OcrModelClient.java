package com.clipforge.ai.api.ocr;

/**
 * OcrModelClient（OCR 模型客户端）。
 *
 * <p>该接口统一图片型知识文档、扫描版 PDF、图片和视频帧中的文字识别能力。
 * 当前阶段只提供跨模块契约，真实 OCR 实现由后续 AI Infrastructure（AI 基础设施）模块提供。
 */
public interface OcrModelClient {

    /**
     * recognize（识别）用于从图片或扫描文档中提取文字。
     *
     * @param request OCR 模型请求
     * @return OCR 模型响应
     */
    OcrModelResponse recognize(OcrModelRequest request);
}

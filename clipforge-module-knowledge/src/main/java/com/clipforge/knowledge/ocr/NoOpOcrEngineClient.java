package com.clipforge.knowledge.ocr;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;

/**
 * NoOpOcrEngineClient（空实现 OCR 客户端）。
 *
 * <p>该实现用于 Phase 7.16（阶段 7.16）的默认兜底，表示系统已经预留 OCR（光学字符识别）入口，但还没有接入真实引擎。
 */
public class NoOpOcrEngineClient implements OcrEngineClient {

    /**
     * recognize（识别）直接返回“未启用”错误。
     *
     * @param request OCR 识别请求
     * @return 不返回结果，直接抛出业务异常
     */
    @Override
    public OcrEngineResponse recognize(OcrEngineRequest request) {
        // 直接拒绝调用，提醒当前阶段还未接入真实 OCR。
        throw new BusinessException(ErrorCode.OCR_DISABLED, "OCR功能暂未启用，请后续接入真实OCR引擎");
    }
}

package com.clipforge.ai.api.speech;

/**
 * SpeechRecognitionModelClient（语音识别模型客户端）。
 *
 * <p>该接口屏蔽不同 ASR（自动语音识别）供应商差异，向业务层提供统一转写能力。
 */
public interface SpeechRecognitionModelClient {

    /**
     * transcribe（转写）用于把音频文件转换为文本。
     *
     * @param request 语音识别请求
     * @return 语音识别响应
     */
    SpeechRecognitionModelResponse transcribe(SpeechRecognitionModelRequest request);
}

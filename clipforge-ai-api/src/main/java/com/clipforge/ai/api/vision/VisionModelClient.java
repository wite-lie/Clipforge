package com.clipforge.ai.api.vision;

/**
 * VisionModelClient（视觉模型客户端）。
 *
 * <p>该接口用于屏蔽 Mock（模拟）视觉模型、LangChain4j（LangChain 的 Java 封装）视觉模型和后续 OpenAI-Compatible（OpenAI 兼容接口）视觉模型的调用差异。
 */
public interface VisionModelClient {

    /**
     * understand（理解图片）用于发起一次非流式 VLM（视觉语言模型）图片理解调用。
     *
     * @param request 视觉模型请求
     * @return 视觉模型响应
     */
    VisionModelResponse understand(VisionModelRequest request);
}

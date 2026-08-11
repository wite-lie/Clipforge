package com.clipforge.memory.extract;

/**
 * MemoryExtractionModelClient（记忆抽取模型客户端）。
 *
 * <p>该接口屏蔽规则抽取之外的 LLM（大语言模型）补充能力。
 */
public interface MemoryExtractionModelClient {

    /**
     * extract（抽取）用于从本次写入上下文中提炼额外记忆。
     *
     * @param request 模型抽取请求
     * @return 模型抽取结果
     */
    MemoryExtractionModelResponse extract(MemoryExtractionModelRequest request);
}

package com.clipforge.model.embedding;

/**
 * EmbeddingModelClient（嵌入模型客户端）。
 *
 * <p>该接口屏蔽 Mock（模拟）嵌入模型和 LangChain4j（LangChain 的 Java 封装）真实嵌入模型差异。
 */
public interface EmbeddingModelClient {

    /**
     * embed（生成嵌入向量）用于把文本列表转换为向量列表。
     *
     * @param request 嵌入模型请求
     * @return 嵌入模型响应
     */
    EmbeddingModelResponse embed(EmbeddingModelRequest request);
}

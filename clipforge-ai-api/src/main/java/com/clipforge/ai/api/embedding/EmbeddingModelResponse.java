package com.clipforge.ai.api.embedding;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * EmbeddingModelResponse（嵌入模型响应）。
 *
 * <p>该对象保存嵌入模型返回的向量、维度和 Token（词元）用量信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingModelResponse {

    /** provider（供应商）表示模型服务来源。 */
    private String provider;

    /** modelName（模型名称）表示实际调用的 Embedding（嵌入）模型。 */
    private String modelName;

    /** dimension（向量维度）表示每个向量包含多少个浮点数。 */
    private Integer dimension;

    /** embeddings（嵌入向量列表）与请求文本列表一一对应。 */
    private List<List<Float>> embeddings;

    /** promptTokens（输入词元数）表示向量化输入文本消耗的 Token（词元），供应商不返回时使用估算值。 */
    private Integer promptTokens;

    /** totalTokens（总词元数）表示本次向量化总消耗，Embedding（嵌入）通常只有输入消耗。 */
    private Integer totalTokens;
}
